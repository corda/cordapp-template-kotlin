import { useReducer, useState } from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';
import { SHOW_FLOWS, HIDE_FLOWS} from "../services/buttons";
import { NODE_ID } from "../services/urls";
import Modal from "./Modal"
import useModal from "../hooks/useModal";
import { makeStyles } from '@material-ui/core/styles';
import Grid from '@material-ui/core/Grid';


const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1,
        paddingLeft: "200px",
        justifyContent: 'left',
    }
}));


export const trimFlowsForDisplay = (text) => {
    let words = text.split(".")
    return words[words.length - 1]
}

function Flows() {
    const classes = useStyles();
    const [buttonText, setButtonText] = useState(SHOW_FLOWS)
    const [shouldDisplayTable, setDisplayTable] = useState(false)
    const { isShowing, flowData, toggle, setModalData } = useModal()
    const [registeredFlows, setRegisteredFlows] = useState([])

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
            if(data.status){
                setRegisteredFlows(data.data.flowInfoList)
                // console.log(registeredFlows)
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

                <Grid item xs={12}>
                    <a type="button"
                       className="btn btn-2"
                       onClick={ () => { listFlows(); changeText( getButtonText() )}}>{buttonText}</a>

                       { shouldDisplayTable &&

                        <table className="pa1">
                            <tbody>
                            {registeredFlows.map((flow, index) => {
                                return <tr key={index}>
                                    <td className="pv2 tl">
                                        <a type="button" onClick={() => {toggle(); setModalData(flow)}} className="bg-transparent bn f4 white grow">{trimFlowsForDisplay(flow.flowName)}</a>
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
                toggle={toggle} />
                </Grid>
            </Grid>
        </div>
    );
}

export default Flows;
