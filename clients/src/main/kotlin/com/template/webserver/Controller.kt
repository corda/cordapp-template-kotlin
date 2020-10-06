package com.template.webserver

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.cordapp.CordappInfo
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.node.NetworkParameters
import net.corda.core.node.NodeDiagnosticInfo
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.io.File
import java.lang.reflect.*
import java.math.BigDecimal
import java.math.BigInteger
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.function.Consumer
import net.corda.core.contracts.ContractState
import net.corda.core.messaging.StateMachineInfo
import net.corda.core.messaging.StateMachineTransactionMapping

import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria

import org.springframework.web.bind.annotation.PostMapping




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
    private var nodeIdentity: String? = ""

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
    fun getCordapps() : APIResponse<NodeDiagnosticInfo> {
        return try {
            APIResponse.success(proxy.nodeDiagnosticInfo())
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting cordapps")
        }
    }

    @GetMapping(value = [ "flows" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getInstalledFlows(@RequestParam(value = "me") nodeName: String) : APIResponse<FlowData> {
        nodeIdentity = nodeName
        val jarFiles =  loadCordappsToClasspath(nodeName)
        val flowInfoList = loadFlowsInfoFromJarFiles(jarFiles, proxy.registeredFlows())
        return try {
            APIResponse.success(FlowData(flowInfoList))
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting flows")
        }
    }

    @GetMapping(value = [ "network-parameters" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getNetworkParameters() : APIResponse<NetworkParameters> {
        return try {
            APIResponse.success(proxy.networkParameters)
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting network parameters")
        }
    }

    @PostMapping(value = [ "vault-states" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun getVaultStates(@RequestBody pageSpecification: PageSpecification): APIResponse<Vault.Page<ContractState>?>? {
        val pageSpec = PageSpecification(pageSpecification.pageNumber + 1, pageSpecification.pageSize)
        return try {
            APIResponse.success(proxy.vaultQueryByWithPagingSpec(ContractState::class.java, QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL), pageSpec))
        } catch (e: Exception) {
            logger.error(e.message)
            APIResponse.error("Error while getting vault states")
        }
    }

    @PostMapping(value = ["/start-flow/"])
    fun startFlow(@RequestBody flowInfo: FlowInfo): APIResponse<String> {
        val clazz = Class.forName(flowInfo.flowName) as Class<out FlowLogic<*>>
        val params = arrayListOf<Any>()
        if (flowInfo.flowParams != null && flowInfo.flowParams.isNotEmpty()) {
            for (flowParam in flowInfo.flowParams) {
                params.add(buildFlowParam(flowParam, nodeIdentity))
            }
        }

        val paramArray = params.toTypedArray()

        return try {
                if (params.size == 0) {
                    APIResponse.success(proxy.startFlowDynamic(clazz).returnValue.getOrThrow().toString())
                } else {
                    val result = proxy.startFlowDynamic(clazz, *paramArray).returnValue.getOrThrow().toString()
                    APIResponse.success(result)
                }
           } catch (e: Exception) {
               logger.error(e.message)
               APIResponse.error("Error while starting flow")
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

    private fun loadFlowParams(flowClass: Class<*>): Map<String, List<FlowParam>> {
        var flowParamList: List<FlowParam?>
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

    private fun buildFlowParam(flowParam: FlowParam, nodeName: String?): Any {
        if (flowParam.flowParams != null && flowParam.flowParams.isNotEmpty()) {
            val params: MutableList<Any> = ArrayList()
            var clazz: Class<*>?
            try {
                clazz = Class.forName(flowParam.paramType.canonicalName)
            } catch (e: ClassNotFoundException) {
                val jarFiles = loadCordappsToClasspath(nodeName!!)
                clazz = loadClassFromCordappJar(flowParam.paramType, jarFiles)
                if (clazz == null) {
                    throw IllegalArgumentException("Cannot load flow class " + flowParam.paramType.toString())
                }
            }
            try {
                for (param in flowParam.flowParams) {
                    params.add(buildFlowParam(param, nodeName))
                }
                for (ctor in clazz!!.constructors) {
                    if (ctor.parameters.size == params.size) return ctor.newInstance(*params.toTypedArray())
                }
            } catch (e: Exception) {
                throw IllegalArgumentException(e.message)
            }
        }
        return when (flowParam.paramType.canonicalName) {
            "net.corda.core.identity.Party" ->  {
                // partiesFromName doesn't work .. que?
                proxy.networkMapSnapshot().flatMap{ it.legalIdentities }.filter { it.name.toString() == flowParam.paramValue as String }.iterator().next()
            }
            "java.lang.String", "java.lang.StringBuilder", "java.lang.StringBuffer" -> flowParam.paramValue.toString()
            "java.lang.Long", "long" -> java.lang.Long.valueOf(flowParam.paramValue.toString())
            "java.lang.Integer", "int" -> Integer.valueOf(flowParam.paramValue.toString())
            "java.land.Double", "double" -> java.lang.Double.valueOf(flowParam.paramValue.toString())
            "java.lang.Float", "float" -> java.lang.Float.valueOf(flowParam.paramValue.toString())
            "java.math.BigDecimal" -> BigDecimal(flowParam.paramValue.toString())
            "java.math.BigInteger" -> BigInteger(flowParam.paramValue.toString())
            "java.lang.Boolean", "boolean" -> java.lang.Boolean.valueOf(flowParam.paramValue.toString())
            "java.util.UUID" -> UUID.fromString(flowParam.paramValue.toString())
            "net.corda.core.contracts.UniqueIdentifier" -> UniqueIdentifier(null, UUID.fromString(flowParam.paramValue.toString()))
            "net.corda.core.contracts.Amount" -> Amount.parseCurrency(flowParam.paramValue.toString())
            "java.time.LocalDateTime" -> LocalDateTime.parse(flowParam.paramValue.toString())
            "java.time.Instant" -> LocalDateTime.parse(flowParam.paramValue.toString()).atZone(ZoneId.systemDefault())
            "java.time.LocalDate" -> LocalDate.parse(flowParam.paramValue.toString())
            "net.corda.core.crypto.SecureHash" -> SecureHash.parse(flowParam.paramValue.toString())
            "net.corda.core.utilities.OpaqueBytes" -> OpaqueBytes(flowParam.paramValue.toString().toByteArray())
            "java.util.List", "java.util.Set" -> buildListParam(flowParam)
            else -> throw IllegalArgumentException("Type " + flowParam.paramType.toString() + " in Flow Parameter not " +
                    "supported by current version of Node Explorer")
        }
    }

    private fun buildListParam(flowParam: FlowParam): Any {
        val paramVal: MutableList<Any> = ArrayList()
        val paramValArr = flowParam.paramValue as ArrayList<Object>
        for (paramObj in paramValArr) {
            val param = FlowParam(paramType = flowParam.parameterizedType, paramValue = paramObj, flowParams = flowParam.flowParams)
            val builtParam = buildFlowParam(param, nodeIdentity)
            paramVal.add(builtParam)
        }
        return paramVal
    }

    private fun loadClassFromCordappJar(clazz: Class<*>, jarFiles: List<File>): Class<*>? {
        var clazz = clazz
        for (jarFile in jarFiles) {
            try {
                val url = jarFile.toURI().toURL()
                val classLoader = URLClassLoader(arrayOf(url), javaClass.classLoader)
                try {
                    clazz = Class.forName(clazz.name, false, classLoader)
                    break
                } catch (e: ClassNotFoundException) {
                    try {
                        clazz = Class.forName(clazz.canonicalName, false, classLoader)
                        break
                    } catch (ce: ClassNotFoundException) {
                    }
                }
            } catch (e: MalformedURLException) {
            }
        }
        return clazz
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
        var paramName: String = "",
        var paramType: Class<*>,
        var flowParams: List<FlowParam> = emptyList(),
        var parameterizedType: Class<*> = Any::class.java,
        var hasParameterizedType: Boolean = false,
        var paramValue: Any? = null
)