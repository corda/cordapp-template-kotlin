import React from "react";
import { FormControl, InputLabel, MenuItem, FormHelperText, TextField, Select} from "@material-ui/core";
import http from "../services/http";
import urls, {NODE_ID} from "../services/urls";
import '../styling/FlowParameters.css';

function FlowParameters({registeredFlow}) {
    const [activeConstructor, setActiveConstructor] = React.useState(["Constructor_1"])
    const [flowParams, setFlowParams] = React.useState([registeredFlow.flowParamsMap["Constructor_1"]])
    const [paramList, setParamList] = React.useState([registeredFlow.flowParams])
    const [parties, setParties] = React.useState([getParties()])

    function handleFlowConstructorSelection(event) {
        setActiveConstructor([event.target.value])
        setFlowParams(registeredFlow.flowParamsMap[event.target.value])
        setParamList(registeredFlow.flowParams)
        console.log(flowParams)
    }

    function getParties() {
        http.get(urls.get_parties)
            .then(r => {
                if(r.status === 200 && r.data.status === true){
                    const filteredParties = r.data.data.filter ( party => !party.includes(NODE_ID) && !party.includes("Notary"))
                    console.log("parties:" + filteredParties)
                    setParties(filteredParties)
                } else {
                }
            });
    }


    function renderParamForm(innerForm, paramList, title, deep, delIdx, param, key){
        return(
            <React.Fragment>
                {
                    innerForm?
                        <div className="inner-form" style={{padding: deep? "10px 0px 0px 0px":  "10px 0"}} key={key}>
                            {/*{*/}
                            {/*    delIdx>=0?<div className="inner-form-close" onClick={()=> this.updateCmplxListParam(param, false, delIdx)}>X</div>:null*/}
                            {/*}*/}
                            <div style={{padding: deep? 0:  "0 10px"}}>
                                <div style={{textTransform:"capitalize"}}><strong>{title}</strong></div>
                                {
                                    paramList.map((param, index) => renderInnerForm(param, index, true))
                                }
                            </div>
                        </div>
                        :
                        flowParams.map((param, index) => renderInnerForm(param, index, false))
                }
            </React.Fragment>
        );
    }

    function renderInnerForm(param, index, deep){
        return(
            param.flowParams && param.flowParams.length > 1 && !(param.hasParameterizedType && (param.paramType === 'java.util.List' || param.paramType === 'java.util.Set'))?
                renderParamForm(true, param.flowParams, param.paramName, deep)
                : // List of complex object
                param.flowParams && param.flowParams.length > 1 && (param.hasParameterizedType && (param.paramType === 'java.util.List' || param.paramType === 'java.util.Set'))?
                    <React.Fragment>
                        <div style={{color: 'red', marginTop: 10}}>List of Complex Object is not supported</div>
                    </React.Fragment>
                    :
                    <React.Fragment>
                        <div key={index} style={{width: "50%", float: "left", marginBottom: 5}}>
                            {
                                param.paramType === 'net.corda.core.identity.Party'?
                                    <div style={{paddingRight: index%2===0? 5:0, paddingLeft: index%2===1? 5:0}}>
                                        <FormControl fullWidth>
                                            <InputLabel>{param.paramName}</InputLabel>
                                            <Select onChange={e => {param.paramValue = e.target.value}} autoWidth>

                                                {
                                                    parties.map((party, index) => {
                                                        return(
                                                            <MenuItem key={index} value={party}>{party}</MenuItem>
                                                        );
                                                    })
                                                }
                                            </Select>
                                            <FormHelperText>Select Party</FormHelperText>
                                        </FormControl>
                                    </div>
                                    :
                                    param.paramType === 'java.time.LocalDateTime' || param.paramType === 'java.time.Instant'?
                                        <div style={{paddingRight: index%2===0? 5:0, paddingLeft: index%2===1? 5:0}}>
                                            <TextField type="datetime-local" onBlur={e=> {param.paramValue = e.target.value}} label={param.paramName} InputLabelProps={{ shrink: true }}
                                                       helperText={getHelperText(param.paramType)} fullWidth/>
                                        </div>
                                        :
                                        param.paramType === 'java.time.LocalDate'?
                                            <div style={{paddingRight: index%2===0? 5:0, paddingLeft: index%2===1? 5:0}}>
                                                <TextField type="date" onBlur={e=> {param.paramValue = e.target.value}} label={param.paramName} InputLabelProps={{ shrink: true }} fullWidth/>
                                            </div>
                                            :
                                            param.hasParameterizedType && (param.paramType === 'java.util.List' || param.paramType === 'java.util.Set') ?
                                                renderListParam(param, index)
                                                :
                                                <div style={{paddingRight: index%2===0? 5:0, paddingLeft: index%2===1? 5:0}}>
                                                    <TextField onBlur={e=> {param.paramValue = e.target.value}} label={param.paramName} helperText={getHelperText(param.paramType)} fullWidth/>
                                                </div>
                            }
                        </div>
                        {
                            index%2 === 1? <div style={{clear: "both"}}></div>: null
                        }
                    </React.Fragment>
        );
    }

    function renderListParam(param, index){
        return (
            <div style={{paddingRight: index%2===0? 5:0, paddingLeft: index%2===1? 5:0}}>
                {
                    param.parameterizedType === 'net.corda.core.identity.Party'?
                        <React.Fragment>
                            <FormControl fullWidth>
                                <InputLabel>{param.paramName}</InputLabel>
                                <Select onChange={e => updateListParam(param, e.target.value, true)} autoWidth>
                                    {
                                        parties.map((party, index) => {
                                            return(
                                                <MenuItem key={index} value={party}>{party}</MenuItem>
                                            );
                                        })
                                    }
                                </Select>
                                <FormHelperText>Select Parties</FormHelperText>
                            </FormControl>
                            {
                                paramList[param.paramName]?
                                    paramList[param.paramName].map((value, idx) => {
                                        return (<div key={idx} className="list-selection">{value}<span onClick={()=>this.updateListParam(param, "", false, idx)}>X</span></div>)
                                    })
                                    :null
                            }
                        </React.Fragment>
                        : param.parameterizedType === 'java.time.LocalDateTime' || param.parameterizedType === 'java.time.Instant'?
                        <React.Fragment>
                            <div style={{paddingRight: index%2===0? 5:0, paddingLeft: index%2===1? 5:0}}>
                                <TextField type="datetime-local" onBlur={e => updateListParam(param, e.target.value, true)} label={param.paramName} InputLabelProps={{ shrink: true }}
                                           helperText={getHelperText(param.paramType)} fullWidth/>
                            </div>
                            {
                                paramList[param.paramName]?
                                    paramList[param.paramName].map((value, idx) => {
                                        return (<div key={idx} className="list-selection">{value}<span onClick={()=>updateListParam(param, "", false, idx)}>X</span></div>)
                                    })
                                    :null
                            }
                        </React.Fragment>
                        :
                        param.parameterizedType === 'java.time.LocalDate'?
                            <React.Fragment>
                                <div style={{paddingRight: index%2===0? 5:0, paddingLeft: index%2===1? 5:0}}>
                                    <TextField type="date" onBlur={e => updateListParam(param, e.target.value, true)} label={param.paramName} InputLabelProps={{ shrink: true }} fullWidth/>
                                </div>
                                {
                                    paramList[param.paramName]?
                                        paramList[param.paramName].map((value, idx) => {
                                            return (<div key={idx} className="list-selection">{value}<span onClick={()=>updateListParam(param, "", false, idx)}>X</span></div>)
                                        })
                                        :null
                                }
                            </React.Fragment>
                            :
                            param.hasParameterizedType && (param.paramType === 'java.util.List' || param.paramType === 'java.util.Set') ?
                                <div style={{color: 'red', marginTop: 10}}>Nested List Param is not supported!</div>
                                :
                                <React.Fragment>
                                    <div style={{paddingRight: index%2===0? 5:0, paddingLeft: index%2===1? 5:0}}>
                                        <TextField onBlur={e => updateListParam(param, e.target.value, true)} label={param.paramName} helperText={getHelperText(param.paramType)} fullWidth/>
                                    </div>
                                    {
                                        paramList[param.paramName]?
                                            paramList[param.paramName].map((value, idx) => {
                                                return (<div key={idx} className="list-selection">{value}<span onClick={()=>updateListParam(param, "", false, idx)}>X</span></div>)
                                            })
                                            :null
                                    }
                                </React.Fragment>
                }
            </div>
        );
    }

    function updateListParam(param, val, flag, idx) {
        if(flag){
            if(param.paramValue === undefined || param.paramValue === null)
                param.paramValue = []

            param.paramValue.push(val);
            let keyVal = [];
            keyVal[param.paramName] = param.paramValue;
            this.setState({
                paramList: keyVal
            });
        }else{
            param.paramValue.splice(idx, 1);
            paramList[param.paramName].splice(idx, 1)
            let keyVal = [];
            keyVal[param.paramName] = paramList[param.paramName];
            this.setState({
                paramList: keyVal
            });

        }
    }

    function getHelperText(paramType){
        switch(paramType){
            case 'net.corda.core.contracts.Amount':
                return 'Param Type: ' + paramType + ' eg: 100 USD';

            case 'java.lang.Boolean':
            case 'boolean':
                return 'Param Type: ' + paramType + ' eg: true or false';

            case 'java.time.LocalDateTime':
            case 'java.time.Instant':
                return 'Param Type: ' + paramType + ' eg: 10/02/2020 10:12:30 AM';

            case 'net.corda.core.utilities.OpaqueBytes':
                return 'Param Type: ' + paramType + ', Enter String value';

            default:
                return 'Param Type: ' + paramType;
        }
    }



    return (
        <div>
            <div style={{width: "30%", float: "left"}}>
            <div>
                {/*{ setFlowParams(registeredFlow.flowParamsMap["Constructor_1"]) }*/}
                {/*{ setParamList(registeredFlow.flowParams) }*/}
            </div>
            {/*{  Object.keys(constructors) > 0 ?*/}
                    <FormControl style={{width:"100%"}}>
                        <div style={{paddingLeft: 10}}>
                            <InputLabel id="flow-cons-select-label" style={{paddingLeft: 10}}>Select A Constructor Type</InputLabel>
                            <Select labelId="flow-cons-select-label" onChange={event => handleFlowConstructorSelection(event)}
                                    value={activeConstructor} fullWidth>
                                {
                                    Object.keys(registeredFlow.flowParamsMap).map((constructor, index) => {
                                        return(
                                            <MenuItem key={index} value={constructor}>{constructor}</MenuItem>
                                        );
                                    })
                                }
                            </Select>
                        </div>
                    </FormControl>
                </div>

            <div>
                {
                    renderParamForm(false)
                }
                <div style={{width: "100%", float:"left", marginTop: 10, scroll: "auto"}}>
                    {/*{*/}
                    {/*    flowResultMsg    ?*/}
                    {/*        <div style={{float: "left", fontSize: 14}}>*/}
                    {/*            <p style={{color: this.props.flowResultMsgType?"green":"red"}}>*/}
                    {/*                <span>{this.props.flowResultMsgType?'Flow Successful :': 'Flow Errored :'}</span>*/}
                    {/*                {this.props.flowResultMsg}*/}
                    {/*            </p>*/}
                    {/*        </div>*/}
                    {/*        :null*/}
                    {/*}*/}
                    {/*{*/}
                    {/*    this.props.flowSelected && Object.keys(this.state.selectedFlow.constructors).length>0?*/}
                    {/*        <Button onClick={() => this.prepareFlowDataToStart()} style={{float: "right", marginTop: 10}}*/}
                    {/*                variant="contained" color="primary" disabled={this.props.flowInFlight}>*/}
                    {/*            {this.props.flowInFlight?'Please Wait...':'Execute'}*/}
                    {/*        </Button>*/}
                    {/*        :null*/}
                    {/*}*/}
                </div>
            </div>
        </div>
    )
}

export default FlowParameters