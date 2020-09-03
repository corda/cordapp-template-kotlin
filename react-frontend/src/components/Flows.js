import { useState } from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';
import '../styling/Flows.css';
import { SHOW_FLOWS, HIDE_FLOWS} from "../services/buttons";
import Modal from "./Modal"
import useModal from "../hooks/useModal"

function Flows() {
    const [flows, setFlows] = useState([])
    const [buttonText, setButtonText] = useState("Flows")
    const [shouldDisplayTable, setDisplayTable] = useState(false)
    const [selectedFlow, setSelectedFlow] = useState("")
    const { isShowing, displayData, toggle, setModalData } = useModal()

     const changeText = (text) => {
        setButtonText(text)
        setDisplayTable(!shouldDisplayTable)
    }
    const getButtonText = () => shouldDisplayTable ? SHOW_FLOWS : HIDE_FLOWS

    const trimFlowsForDisplay = (text) => {
        var words = text.split(".")
        console.log("words" + words)
        return words[words.length - 1]
    }

    function listFlows() {
        console.log("Getting flows");
        http.get(urls.get_flows)
            .then(r => {
                if(r.status === 200 && r.data.status === true){
                    console.log("flows:" + r.data.data)
                    setFlows(r.data.data.filter( flow => !flow.includes('ContractUpgrade')))
                } else {
                }
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
                    {flows.map((flow, index) => {
                        return <tr>
                            <td className="pv2 tl"key={index}>
                                {/*eslint-disable-next-line*/}
                                <a type="button"
                                   onClick={() => {toggle(); setModalData(flow)}}
                                   className="bg-transparent bn f4 white grow">{trimFlowsForDisplay(flow)}</a>
                            </td>
                        </tr>
                    })}
                    </tbody>
                </table>
            }
            <Modal
                flow={displayData}
                isShowing={isShowing}
                hide={toggle} />
        </div>
    );
}
export default Flows;
