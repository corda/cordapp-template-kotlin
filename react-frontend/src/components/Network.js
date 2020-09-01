import { useState } from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';
import { NODE_ID } from "../services/urls";



function Network() {
    const [parties, setParties] = useState([])
    const [buttonText, setButtonText] = useState("Show network participants")
    const [shouldDisplayTable, setDisplayTable] = useState(false)
    const changeText = (text) => {
        setButtonText(text)
        setDisplayTable(!shouldDisplayTable)
    }
    const getButtonText = () => shouldDisplayTable ? "Show network participants" : "Hide network participants"

    function getParties() {
        console.log("Getting parties");
        http.get(urls.get_parties)
            .then(r => {
                if(r.status === 200 && r.data.status === true){
                const ps = r.data.data.filter ( party => party != NODE_ID)
                console.log("parties:" + ps)
                    setParties(r.data.data.filter ( party => party != NODE_ID))
                } else {
                }
            });
    }

    return (
        <div>
            <a type="button"
                className="btn btn-2"
                onClick={ () => { getParties(); changeText( getButtonText() )}}>{buttonText}</a>
            { shouldDisplayTable &&
            <table className="pa4 tl">
                <tbody>
                {parties.map((party, index) => {
                    return <tr>
                        <td className="pv2" key={index}>
                            <button className="bg-transparent bn f4 white grow">
                                {party}
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
export default Network;
