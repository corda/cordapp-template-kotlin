import { useState } from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';

function Flows() {
    const [flows, setFlows] = useState([])
    const [buttonText, setButtonText] = useState("Show flows")
    const [shouldDisplayTable, setDisplayTable] = useState(false)
    const changeText = (text) => {
        setButtonText(text)
        setDisplayTable(!shouldDisplayTable)
    }
    const getButtonText = () => shouldDisplayTable ? "Show flows" : "Hide flows"

    function listFlows() {
        console.log("Getting flows");
        http.get(urls.get_flows)
            .then(r => {
                if(r.status === 200 && r.data.status === true){
                    console.log("flows:" + r.data.data)
                    setFlows(r.data.data)
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
                                <button className="bg-transparent bn f4 white">
                                    {flow}
                                </button>
                            </td>
                        </tr>
                    })}
                    </tbody>
                </table>
            }
        </div>
    );
}
export default Flows;
