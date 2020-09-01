import { useState } from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';
import '../styling/Cordapps.css';


function Cordapps() {
    const [cordapps, setCordapps] = useState([])
    const [buttonText, setButtonText] = useState("Show network participants")
    const [shouldDisplayTable, setDisplayTable] = useState(false)
    const changeText = (text) => {
        setButtonText(text)
        setDisplayTable(!shouldDisplayTable)
    }
    const getButtonText = () => shouldDisplayTable ? "Show network participants" : "Hide network participants"

    function listCordapps() {
        console.log("Getting flows");
        http.get(urls.get_cordapps)
            .then(r => {
                if(r.status === 200 && r.data.status === true){
                    console.log("flows:" + r.data.data)
                    setCordapps(r.data.data)
                } else {
                }
            });
    }

    return (
        <div>
            <a type="button"
               className="btn btn-2"
               onClick={ () => { listCordapps(); changeText( getButtonText() )}}>{buttonText}</a>
            { shouldDisplayTable &&
                <table className="pa4">
                    <tbody>
                    {cordapps.map((cordapp, index) => {
                        return <tr>
                            <div key={index} className="appInfo-wrapper">
                                <div className="appInfo" style={{marginRight: index%2===0?5:0, marginLeft: index%2===0?0:5}}>
                                    <div>{cordapp.shortName}</div>
                                    <div><span>Version: </span> {cordapp.version}</div>
                                    <div><span>Type: </span> {cordapp.type}</div>
                                    <div><span>Minimum Platform Version: </span> {cordapp.minimumPlatformVersion}</div>
                                    <div><span>Target Platform Version: </span> {cordapp.targetPlatformVersion}</div>
                                    <div><span>File: </span> {cordapp.name}.jar</div>
                                    <div><span>Vendor: </span> {cordapp.vendor}</div>
                                    <div><span>License: </span> {cordapp.licence}</div>
                                </div>
                            </div>
                        </tr>
                    })}
                    </tbody>
                </table>
            }
        </div>
    );
}
export default Cordapps;