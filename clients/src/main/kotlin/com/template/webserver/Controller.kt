package com.template.webserver

import net.corda.core.cordapp.CordappInfo
import net.corda.core.utilities.OpaqueBytes
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer

/**
 * Define your API endpoints here.
 */
@CrossOrigin(origins = arrayOf("*"), maxAge = 3600) //Bit of a hack
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = [ "parties" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getParties() : APIResponse<List<String>> {
        return try {
            APIResponse.success(proxy.networkMapSnapshot().map{ it.legalIdentities.first().name.toString()})
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting parties")
        }
    }


    @GetMapping(value = [ "cordapps" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCordapps() : APIResponse<List<CordappInfo>> {
        return try {
            APIResponse.success(proxy.nodeDiagnosticInfo().cordapps)
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting cordapps")
        }
    }

    @GetMapping(value = [ "flows" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getInstalledFlows(@RequestParam(value = "me") nodeName: String) : APIResponse<FlowData> {
        val jarFiles = loadCordappsToClasspath(nodeName)
        val flowInfoList = loadFlowsInfoFromJarFiles(jarFiles, proxy.registeredFlows())
        return try {
            APIResponse.success(FlowData(flowInfoList))
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting flows")
        }
    }





    private fun loadFlowsInfoFromJarFiles(jarFiles: List<File>, registeredFlows: List<String>): List<FlowInfo> {
        val flowInfoList: MutableList<FlowInfo> = ArrayList()
        for (flow in registeredFlows) {
            if (!flow.contains("net.corda.core.flows") && !flow.contains("com.r3.corda.lib")) {
                for (jarFile in jarFiles) {
                    try {
                        val url = jarFile.toURI().toURL()
                        val classLoader = URLClassLoader(arrayOf(url), javaClass.classLoader)
                        try {

                            val flowInfo = FlowInfo(flowName = flow)
                            val flowClass = Class.forName(flow, true, classLoader)
                            flowInfo.flowParamsMap = loadFlowParams(flowClass)
                            flowInfoList.add(flowInfo)
                            break
                        } catch (e: ClassNotFoundException) {
                            e.printStackTrace()
                        }
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return flowInfoList
    }

    // TODO: Handle multiple constructors
    private fun loadFlowParams(flowClass: Class<*>): Map<String, List<FlowParam>> {
        var flowParamList: List<FlowParam?> = ArrayList()
        val conMap: MutableMap<String, List<FlowParam>> = LinkedHashMap()
        var counter = 1

        // construct params List for each constructor
        for (constructor in flowClass.constructors) {
            if (constructor.parameters.isNotEmpty() && constructor.parameters[0].isNamePresent) {
                try {
                    flowParamList = collectObjectTypes(constructor.parameters)
                    conMap["Constructor_$counter"] = flowParamList
                    counter++
                } catch (cne: ClassNotFoundException) {
                    cne.printStackTrace()
                } catch (cne: RuntimeException) {
                    cne.printStackTrace()
                }
            }
        }
        return conMap
    }

    @Throws(ClassNotFoundException::class)
    private fun collectObjectTypes(parameters: Array<Parameter>): List<FlowParam> {
        val flowParamList: MutableList<FlowParam> = ArrayList()
        for (param in parameters) {
            if (param.isNamePresent) {
                val flowParam = FlowParam(paramName = param.name, paramType = param.type)
                if (param.type == MutableList::class.java || param.type == MutableSet::class.java) {
                    try {
                        val type = Class.forName((param.parameterizedType as ParameterizedType).actualTypeArguments[0].typeName)
                        flowParam.parameterizedType = type
                        flowParam.hasParameterizedType = true
                        removeKotlinDefaultConstructorAndCollectParam(type, flowParam)
                    } catch (cne: ClassNotFoundException) {
                        throw java.lang.RuntimeException("Flow param unsupported by Node Explorer see https://github.com/corda/node-explorer/releases")
                    }
                } else removeKotlinDefaultConstructorAndCollectParam(param.type, flowParam)
                flowParamList.add(flowParam)
            }
        }
        return flowParamList
    }

    private val typeList: List<String> = ArrayList(
            Arrays.asList("net.corda.core.identity.Party",
                    "java.lang.String",
                    "java.lang.StringBuilder",
                    "java.lang.StringBuffer",
                    "java.lang.Long",
                    "long",
                    "java.lang.Integer",
                    "int",
                    "java.land.Double",
                    "double",
                    "java.lang.Float",
                    "float",
                    "java.math.BigDecimal",
                    "java.math.BigInteger",
                    "java.lang.Boolean",
                    "boolean",
                    "java.util.UUID",
                    "net.corda.core.contracts.UniqueIdentifier",
                    "net.corda.core.contracts.Amount",
                    "java.time.LocalDateTime",
                    "java.time.LocalDate",
                    "java.time.Instant",
                    "net.corda.core.crypto.SecureHash",
                    "java.util.List",
                    "java.util.Set",
                    "net.corda.core.utilities.OpaqueBytes"
            )
    )

    @Throws(ClassNotFoundException::class)
    private fun removeKotlinDefaultConstructorAndCollectParam(type: Class<*>, flowParam: FlowParam) {
        if (!typeList.contains(type.canonicalName)) {
            for (i in type.constructors.indices) {
                var collected = false
                collected = if (type.constructors[i].parameters.size > 0 &&
                        type.constructors[i].parameters[0].isNamePresent && !collected) {
                    flowParam.flowParams = collectObjectTypes(type.constructors[0].parameters)
                    true
                } else {
                    continue
                }
            }
        }
    }


    private fun loadCordappsToClasspath(nodeName: String): List<File> {
        // TODO This will break if you change REACT_APP_USER_ID in package.json to something other than the node names in deployNodes task
        val cordappPath = Paths.get("").toAbsolutePath().parent.toString().plus("/build/nodes/${nodeName}/cordapps")
        logger.info("CONTROLLER PATH:  $cordappPath")
        val jarFiles = filterJarFiles(cordappPath)
        addJarFilesToClassPath(jarFiles)
        return jarFiles
    }

    private fun filterJarFiles(path: String): List<File> {
        val dir = File(path)
        val jarFiles: MutableList<File> = ArrayList()
        val filesList = dir.listFiles()
        if (filesList != null && filesList.isNotEmpty()) {
            Arrays.stream(filesList).filter { file: File -> file.name.contains(".jar") }
                    .forEach { e: File -> jarFiles.add(e) }
        }
        return jarFiles
    }

    private fun addJarFilesToClassPath(jarFiles: List<File>) {
        val sysClassLoader = this.javaClass.classLoader as URLClassLoader
        try {
            val method = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
            jarFiles.forEach(Consumer { file: File ->
                try {
                    val url = file.toURI().toURL()
                    method.isAccessible = true
                    method.invoke(sysClassLoader, url)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                }
            })
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        }
    }
}

data class FlowData (val flowInfoList: List<FlowInfo>)

data class FlowInfo(
    val flowName: String = "",
    var flowParams: List<FlowParam> = emptyList(),
    var flowParamsMap: Map<String, List<FlowParam>> = emptyMap()
)

data class FlowParam(
        var paramName: String,
        var paramType: Class<*>,
        var flowParams: List<FlowParam> = emptyList(),
        var parameterizedType: Class<*> = Any::class.java,
        var hasParameterizedType: Boolean = false
//        var paramValue: Object = 0
    ) {
}