import React, {useState} from 'react';
import '../styling/Vault.css';
import urls from "../services/urls";
import { Grid, TablePagination } from '@material-ui/core';

function Vault() {
    const [states, setStates] = useState([])
    const [statesMetaData, setStatesMetaData] = useState([])
    const [totalStatesAvailable, setTotalStatesAvailable] = useState(0)
    // const [stateTypes, setStateTypes] = useState("")

    // val states: List<StateAndRef<T>>,
    // val statesMetadata: List<StateMetadata>,
    // val totalStatesAvailable: Long,
    // val stateTypes: StateStatus,
    // val otherResults: List<Any>)

    const webSocket = new WebSocket(urls.websocket);
    console.log("websocket: " + urls.websocket)
    webSocket.onopen = () => console.log('opened')
    webSocket.onerror = () => console.log('error')
    webSocket.onmessage = function(data) {
        console.log(data.data);
        let parsedData = JSON.parse(data.data);
        console.log(parsedData.eventName)
        if(parsedData.eventName === "VAULT_UPDATE") {
            setStates(parsedData.vault.states)
            setStatesMetaData(parsedData.vault.statesMetadata)
            setTotalStatesAvailable(parsedData.vault.totalStatesAvailable)
        }
        else {
            console.log("something other than a vault update event came from the WS ")
        }
    };

    function renderJson(jsonObj, lvl) {
        return(
            Object.keys(jsonObj).filter((key) => key !== "@class").map((key) => {
                return (
                    jsonObj[key] ?
                        <div style={{marginLeft: lvl * 15, paddingBottom: lvl === 0?5:0}}>
                            {lvl === 0?
                                <span><strong>{key}: &nbsp;</strong></span>
                                :
                                <span>{key}: &nbsp;</span>
                            }

                            {typeof jsonObj[key] === 'object'?
                                renderJson(jsonObj[key], lvl+1)
                                :
                                jsonObj[key]+""}
                        </div>:null
                )
            })
        )
    }

    return (

        <React.Fragment>
            <div style={{padding: "20px 20px 10px"}}>
                <div className="page-title">
                    <span>Vault</span>
                </div>
            </div>
            <Grid container spacing={0}>
                <Grid item xs={3}>
                    <div> Filter went here</div>
                </Grid>
                <Grid item xs={9}>
                    {
                        states?
                            states.map((state, idx) => {
                                return (
                                    <div className="state-wrapper">
                                        <div className="state-title">
                                            <div style={{display:"inline=block"}}>{statesMetaData?statesMetaData[idx].contractStateClassName:null}</div>
                                            <div className="tx">StateRef: {state.ref.txhash}({state.ref.index})</div>
                                        </div>
                                        <Grid container spacing={0} style={{padding:10}}>
                                            <Grid item xs={9}>
                                                <div className="state-content">
                                                    {renderJson(state.state.data, 0)}
                                                </div>
                                            </Grid>
                                            <Grid item xs={3}>
                                                {
                                                    statesMetaData?
                                                        <React.Fragment>
                                                            <div className="bar">
                                                                <div className={statesMetaData[idx].relevancyStatus==='RELEVANT'?'blue':'grey'}>{statesMetaData[idx].relevancyStatus}</div>
                                                                <div className={statesMetaData[idx].status==='CONSUMED'?'red':'green'}>{statesMetaData[idx].status}</div>
                                                            </div>
                                                            <div className="meta-container">
                                                                <div><span><strong>Contract: &nbsp;</strong></span> {state.state.contract}</div>
                                                                <div><span><strong>Recorded Time: &nbsp;</strong></span> {statesMetaData[idx].recordedTime}</div>
                                                                {statesMetaData[idx].consumedTime?
                                                                    <div><span><strong>ConsumedTime: &nbsp;</strong></span> {statesMetaData[idx].consumedTime}</div>
                                                                    :null
                                                                }
                                                                <div><span><strong>Notary: &nbsp;</strong></span> {statesMetaData[idx].notary}</div>
                                                            </div>
                                                        </React.Fragment>
                                                        :null
                                                }
                                            </Grid>
                                        </Grid>
                                    </div>
                                )
                            }): null

                    }
                    {
                        !states || states.length === 0?
                            <div className="empty">No States Recorded in The Vault</div>:null
                    }
                    {
                        <TablePagination style= {{padding: "0 10px", marginTop: -15}}
                                         rowsPerPageOptions={[]}
                                         component="div"
                                         count={totalStatesAvailable}
                                         // rowsPerPage={this.state.filter.pageSize}
                                         // page={this.state.filter.offset}
                                         // onChangePage={this.handleChangePage}
                        />
                    }
                </Grid>
            </Grid>

        </React.Fragment>
    );
}
export default Vault;
