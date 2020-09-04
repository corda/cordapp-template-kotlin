import { useReducer, useState } from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';
import { SHOW_FLOWS, HIDE_FLOWS} from "../services/buttons";
import { NODE_ID } from "../services/urls";
import Modal from "./Modal"
import useModal from "../hooks/useModal"

export const FlowContext = React.createContext({
    registeredFlow: []
})

const trimFlowsForDisplay = (text) => {
    var words = text.split(".")
    return words[words.length - 1]
}

function Flows() {
    const [buttonText, setButtonText] = useState("Flows")
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
                console.log(registeredFlows)
            } else {

            }
        }).catch(error => {
            console.log(error)
        });
    }

    return (
        <div>
            <a type="button"
               className="btn btn-2"
               onClick={ () => { listFlows(); changeText( getButtonText() )}}>{buttonText}</a>
            { shouldDisplayTable &&
                <table className="pa4">
                    <tbody>
                    {registeredFlows.map((flow, index) => {
                        return <tr key={index}>
                            <td className="pv2 tl">
                                {/*eslint-disable-next-line*/}
                                <a type="button" onClick={() => {toggle(); setModalData(flow)}} className="bg-transparent bn f4 white grow">{trimFlowsForDisplay(flow.flowName)}</a>
                            </td>
                        </tr>
                    })}
                    </tbody>
                </table>
            }
            <div>
            <ul>
                {registeredFlows.map(flowInfo => {
                    console.log(flowInfo)
                    })
                }
            </ul>
            </div>
            <Modal
                registeredFlow={flowData}
                isShowing={isShowing}
                hide={toggle} />
        </div>
    );
}
export default Flows;
