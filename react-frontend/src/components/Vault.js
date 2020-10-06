import React, {useState, useEffect} from 'react';
import '../styling/Vault.css';
import urls, {NODE_ID} from "../services/urls";
import { Grid, TablePagination } from '@material-ui/core';
import http from "../services/http";

const PAGE_SIZE = 5

function Vault() {
    const [ stateData, setStateData ] = useState({
        state: [],
        stateMetadata: [],
        totalStatesAvailable: 0,
    })
    const [pageSpec, setPageSpec] = useState({
        pageNumber: 0,
        pageSize: PAGE_SIZE
    })

    useEffect(() => {
        getVaultStates(pageSpec)
    }, [pageSpec]);

    function getVaultStates(pageSpec) {
        http.post(urls.get_vault_states, pageSpec)
            .then(({data}) => {
                if(data.status){
                    let responseData = data.data
                    setStateData({
                        states: responseData.states,
                        stateMetadata: responseData.statesMetadata,
                        totalStatesAvailable: responseData.totalStatesAvailable
                    })
                } else {
                    console.log(data.status)
                }
            }).catch(error => {
                console.log("Something went terribly wrong")
                console.error(error)
        });
    }

    const handleChangePage = (event, newPage) => {
       setPageSpec({
             ...pageSpec,
            pageNumber: newPage
       })
    }

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
                <Grid item xs={1}>
                </Grid>
                <Grid item xs={9}>
                    {
                        stateData.states?
                            stateData.states.map((state, idx) => {
                                return (
                                    <div className="state-wrapper">
                                        <div className="state-title">
                                            <div style={{display:"inline=block"}}>{stateData.stateMetadata ? stateData.stateMetadata[idx].contractStateClassName:null}</div>
                                            <div className="tx">StateRef: {state.ref.txhash}({state.ref.index})</div>
                                        </div>
                                        <Grid container spacing={0} style={{padding:10}}>
                                            <Grid item xs={7}>
                                                <div className="state-content">
                                                    {renderJson(state.state.data, 0)}
                                                </div>
                                            </Grid>
                                            <Grid item xs={5}>
                                                {
                                                    stateData.stateMetadata?
                                                        <React.Fragment>
                                                            <div className="bar">
                                                                <div className={stateData.stateMetadata[idx].relevancyStatus==='RELEVANT'?'blue':'grey'}>{stateData.stateMetadata[idx].relevancyStatus}</div>
                                                                <div className={stateData.stateMetadata[idx].status==='CONSUMED'?'red':'green'}>{stateData.stateMetadata[idx].status}</div>
                                                            </div>
                                                            <div className="meta-container">
                                                                <div><span><strong>Contract: &nbsp;</strong></span> {state.state.contract}</div>
                                                                <div><span><strong>Recorded Time: &nbsp;</strong></span> {stateData.stateMetadata[idx].recordedTime}</div>
                                                                {stateData.stateMetadata[idx].consumedTime?
                                                                    <div><span><strong>ConsumedTime: &nbsp;</strong></span> {stateData.stateMetadata[idx].consumedTime}</div>
                                                                    :null
                                                                }
                                                                <div><span><strong>Notary: &nbsp;</strong></span> {stateData.stateMetadata[idx].notary}</div>
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
                        !stateData.states || stateData.states.length === 0?
                            <div className="empty">No States Recorded in The Vault</div>:null
                    }
                    {
                        <TablePagination style= {{padding: "0 10px", marginTop: -15, color: "white"}}
                                         rowsPerPageOptions={[]}
                                         component="div"
                                         count={stateData.totalStatesAvailable}
                                         rowsPerPage={pageSpec.pageSize}
                                         page={pageSpec.pageNumber}
                                         onChangePage={handleChangePage}
                        />
                    }
                </Grid>
            </Grid>
        </React.Fragment>
    );
}
export default Vault;
