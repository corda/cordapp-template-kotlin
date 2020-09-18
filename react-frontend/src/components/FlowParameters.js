import React, {useContext} from "react";
import { FormControl, InputLabel, MenuItem, FormHelperText, TextField, Select, Button, Grid} from "@material-ui/core";
import http from "../services/http";
import urls, {NODE_ID} from "../services/urls";
import '../styling/FlowParameters.css';
import { useState, useEffect } from 'react';
import { CompletedFlowContext, trimFlowsForDisplay} from "./Flows";
import createPersistedState from 'use-persisted-state';
import { transformPartyName } from "./NetworkParticipants"

function FlowParameters({registeredFlow}) {
    const [activeConstructor, setActiveConstructor] = useState("")
    const [flowParams, setFlowParams] = useState([])
    const [paramList, setParamList] = useState([registeredFlow.flowParams])
    const [parties, setParties] = useState([])
    const [flowResultMsg, setFlowResultMsg] = useState("")
    const [flowCompletionStatus, setFlowCompletionStatus] = useState(false)
    const [isFlowInProgress, setFlowInProgress] = useState(false)
    const useCompletedFlowState = createPersistedState('completedFlows');
    const [completedFlows, setCompletedFlows] = useCompletedFlowState([]);

    function handleFlowConstructorSelection(event) {
        setActiveConstructor([event.target.value])
        setFlowParams(registeredFlow.flowParamsMap[event.target.value])
        setParamList(registeredFlow.flowParams)
    }

    function getParties() {
        http.get(urls.get_parties)
            .then(r => {
                if(r.status === 200 && r.data.status === true){
                    const filteredParties = r.data.data.filter ( party => !party.includes(NODE_ID) && !party.includes("Notary"))
                    setParties(filteredParties)
                } else {
                }
            });
            return parties
    }

    function renderParamForm(innerForm, paramList, title, deep, delIdx, param, key){
        return(
            <React.Fragment>
                {
                    innerForm?
                        <div className="inner-form" style={{padding: deep? "10px 0px 0px 0px":  "10px 0"}} key={key}>
                            {
                                delIdx>=0?<div className="inner-form-close" onClick={()=> updateCmplxListParam(param, false, delIdx)}>X</div>:null
                            }
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
                    <>
                        <div style={{color: 'red', marginTop: 10}}>List of Complex Object is not supported</div>
                    </>
                    :
                    <React.Fragment key={index}>
                        <div key={index} style={{width: "50%", float: "left", marginBottom: 5}}>
                            {
                                param.paramType === 'net.corda.core.identity.Party'?
                                    <div style={{paddingRight: index%2===0? 5:0, paddingLeft: index%2===1? 5:0}}>
                                        <FormControl fullWidth>
                                            <InputLabel>{param.paramName}</InputLabel>
                                            <Select onChange={e => {param.paramValue = e.target.value}} autoWidth defaultValue={''}>
                                                {

                                                    getParties().map((party, index) => {
                                                        return(
                                                            <MenuItem key={index} value={party + ''}>{transformPartyName(party)}</MenuItem>
                                                        );
                                                    })
                                                }
                                            </Select>
                                            <FormHelperText>Select Party</FormHelperText>
                                        </FormControl>
                                    </div>
                                    :
                                    param.paramType === 'java.time.LocalDateTime' || param.paramType === 'java.time.Instant'?
                                        <div style={{paddingRight: index%2===0? 10:0, paddingLeft: index%2===1? 5:0}}>
                                            <TextField type="datetime-local" onBlur={e=> {param.paramValue = e.target.value}} label={param.paramName} InputLabelProps={{ shrink: true, margin: 'dense' }}
                                                       helperText={getHelperText(param.paramType)} fullWidth/>
                                        </div>
                                        :
                                        param.paramType === 'java.time.LocalDate'?
                                            <div style={{paddingRight: index%2===0? 10:0, paddingLeft: index%2===1? 5:0}}>
                                                <TextField type="date" onBlur={e=> {param.paramValue = e.target.value}} label={param.paramName} InputLabelProps={{ shrink: true, margin: 'dense' }} fullWidth/>
                                            </div>
                                            :
                                            param.hasParameterizedType && (param.paramType === 'java.util.List' || param.paramType === 'java.util.Set') ?
                                                renderListParam(param, index)
                                                :
                                                <div style={{paddingRight: index%2===0? 10:0, paddingLeft: index%2===1? 5:0}}>
                                                    <TextField onBlur={e=> {param.paramValue = e.target.value}} label={param.paramName} InputLabelProps={{ shrink: true, margin: 'dense' }} helperText={getHelperText(param.paramType)} fullWidth/>
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
                                <Select onChange={e => updateListParam(param, e.target.value, true)} autoWidth defaultValue={''}>
                                    {
                                        getParties().map((party, index) => {
                                            return(
                                                <MenuItem key={index} value={party}>{transformPartyName(party)}</MenuItem>
                                            );
                                        })
                                    }
                                </Select>
                                <FormHelperText>Select Parties</FormHelperText>
                            </FormControl>
                            {
                                paramList[param.paramName]?
                                    paramList[param.paramName].map((value, idx) => {
                                        return (<div key={idx} className="list-selection">{transformPartyName(value)}<span onClick={()=>updateListParam(param, "", false, idx)}>X</span></div>)
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

    return (
        <div>
            <Grid container spacing={3}>
                <Grid item xs={12}>
                <div style={{width: "30%", float: "left"}}>
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
                </Grid>
            </Grid>
            <div>
                {
                    renderParamForm(false)
                }
                <div style={{width: "100%", float:"left", marginTop: 10, scroll: "auto"}}>
                    {
                        flowCompletionStatus    ?
                            <div style={{float: "left", fontSize: 14}}>
                                <p style={{color: flowCompletionStatus?"green":"red"}}>
                                    <span>{flowCompletionStatus?'Flow Successful': 'Flow Errored'}</span>
                                </p>
                            </div>
                            :null
                    }
                    {
                        activeConstructor?
                            <Button onClick={() => startFlow()} style={{float: "right", marginTop: 10}}
                                    variant="contained" color="primary" disabled={isFlowInProgress}>
                                {isFlowInProgress?'Please Wait...':'Execute'}
                            </Button>
                            :null
                    }
                </div>
            </div>
        </div>
    )

    function startFlow(){
        setFlowInProgress(true)
        let flowInfo = {
            flowName: registeredFlow.flowName,
            flowParams: flowParams
        }

        http.post(urls.start_flow, flowInfo)
            .then(({data}) => {
            if(data.status){
                console.log(data)
                setFlowInProgress(false)
                setFlowCompletionStatus(true)
                setFlowResultMsg(data.data)
                let flowName = trimFlowsForDisplay(registeredFlow.flowName)
                let newFlow = { flowName: flowName, flowCompletionStatus: true }
                setCompletedFlows([...completedFlows, newFlow])
                const jsonString = JSON.stringify(completedFlows)
                localStorage.setItem("completedFlows", jsonString);
                // dispatch({type: "ADD_COMPLETED_FLOW", payload: { completedFlow }})
            } else {
                // dispatch({type: "ADD_COMPLETED_FLOW", payload: { completedFlow }})
            }
        }).catch(error => {
            console.log("FLOW FAILED!!!!")
            setFlowInProgress(false)
            setFlowCompletionStatus(false)
            let flowName = trimFlowsForDisplay(registeredFlow.flowName)
            let newFlow = { flowName: flowName, flowCompletionStatus: true }
            setCompletedFlows([...completedFlows, newFlow])
            const jsonString = JSON.stringify(completedFlows)
            localStorage.setItem("completedFlows", jsonString);
        });
    }

    function updateListParam(param, val, flag, idx) {
        if(flag){
            if(param.paramValue === undefined || param.paramValue === null)
                param.paramValue = []

            param.paramValue.push(val);
            let keyVal = [];
            keyVal[param.paramName] = param.paramValue;
            setParamList(keyVal)
        }else{
            param.paramValue.splice(idx, 1);
            paramList[param.paramName].splice(idx, 1)
            let keyVal = [];
            keyVal[param.paramName] = paramList[param.paramName];
            setParamList(keyVal)

        }
    }

    function updateCmplxListParam(param, flag, idx){
        if(flag){
            let obj = JSON.parse(JSON.stringify(param.paramValue[0]));
            param.paramValue.push(obj);
            let keyVal = [];
            if(!(paramList[param.paramName] === undefined || paramList[param.paramName] === null)){
                keyVal[param.paramName] = paramList[param.paramName];
            }else{
                keyVal[param.paramName] = [];
            }
            if(keyVal[param.paramName].length === 0){
                obj.key = 0;
            }else{
                obj.key = keyVal[param.paramName][keyVal[param.paramName].length -1].key + 1;
            }
            keyVal[param.paramName].push(obj);
            setParamList(keyVal)
        }else{
            param.paramValue.splice(idx+1, 1);
            paramList[param.paramName].splice(idx, 1);
            let keyVal = [];
            keyVal[param.paramName] = this.state.paramList[param.paramName];
            setParamList(keyVal)
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
}

export default FlowParameters