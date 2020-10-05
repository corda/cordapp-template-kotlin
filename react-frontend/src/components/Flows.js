import {useState, createContext, useReducer, useContext} from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';
import {SHOW_FLOWS, HIDE_FLOWS} from "../services/buttons";
import {NODE_ID} from "../services/urls";
import Modal from "./Modal"
import useModal from "../hooks/useModal";
import {makeStyles} from '@material-ui/core/styles';
import Grid from '@material-ui/core/Grid';
import CompletedFlows from "./CompletedFlows";

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1,
        paddingLeft: "200px",
        justifyContent: 'left',
    },
    empty: {
        height: '300px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '20px',
        color: 'white'
    }
}));

export const trimFlowsForDisplay = (text) => {
    let words = text.split(".")
    return words[words.length - 1]
}

const completedFlowsLocal = (key) => {
    if (localStorage.getItem(key) !== null) {
       return JSON.parse(localStorage.getItem(key))
    } else {
        return []
    }
}

function Flows() {
    const classes = useStyles();
    const [buttonText, setButtonText] = useState(SHOW_FLOWS)
    const [shouldDisplayTable, setDisplayTable] = useState(false)
    const [registeredFlows, setRegisteredFlows] = useState([])
    const {isShowing, flowData, toggle, setModalData} = useModal()
    const [refresh, setRefresh] = useState(false)

    const changeText = (text) => {
        setButtonText(text)
        setDisplayTable(!shouldDisplayTable)
    }
    const getButtonText = () => shouldDisplayTable ? SHOW_FLOWS : HIDE_FLOWS

    function listFlows() {
        console.log("Getting flows");
        http.get(urls.get_flows, {
            params: {
                me: NODE_ID
            }
        }).then(({data}) => {
            if (data.status) {
                setRegisteredFlows(data.data.flowInfoList)
            } else {

            }
        }).catch(error => {
            console.log(error)
        });
    }

    return (
        <div className={classes.root}>
                <Grid
                    container
                    direction="row"
                    justify="center"
                    alignItems="stretch"
                    spacing={3}>

                    <Grid item xs={3}>
                        <a type="button"
                           className="btn btn-2"
                           onClick={() => {listFlows();changeText(getButtonText())}}>{buttonText}
                        </a>

                        {shouldDisplayTable &&

                        <table className="pa1">
                            <tbody>
                            {registeredFlows.map((flow, index) => {
                                return <tr key={index}>
                                    <td className="pv2 tl">
                                        <a type="button" onClick={() => {
                                            toggle();
                                            setModalData(flow)
                                        }}
                                           className="bg-transparent bn f4 white grow">{trimFlowsForDisplay(flow.flowName)}</a>
                                    </td>
                                </tr>
                            })}
                            </tbody>
                        </table>
                        }
                        <div>
                        </div>
                        <Modal
                            registeredFlow={flowData}
                            isShowing={isShowing}
                            toggle={toggle}>
                        </Modal>
                    </Grid>
                    <Grid item xs={9}>
                        {
                            completedFlowsLocal("completedFlows").length === 0 ? <div className={classes.empty}>No flows have been executed</div> :
                                <CompletedFlows flows={completedFlowsLocal("completedFlows")} setRefresh={setRefresh}/>
                        }
                    </Grid>
                </Grid>
        </div>
    );
}

export default Flows;
