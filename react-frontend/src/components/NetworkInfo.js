import { useState } from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';
import { NODE_ID } from "../services/urls";
import { SHOW_NETWORK_INFO, HIDE_NETWORK_INFO} from "../services/buttons";

function NetworkInfo() {
    const [parties, setParties] = useState([])
    const [buttonText, setButtonText] = useState("Show Network Info")
    const [shouldDisplayTable, setDisplayTable] = useState(false)

    const changeText = (text) => {
        setButtonText(text)
        setDisplayTable(!shouldDisplayTable)
    }

    const getButtonText = () => shouldDisplayTable ? SHOW_NETWORK_INFO : HIDE_NETWORK_INFO

    function openApplicationWindow(party) {
        window.open(getUrlForParty(party), "_blank")
    }

    function getUrlForParty(party) {
        // Switch doesn't work for some reason
        if (party.includes('PartyA')) {
            return urls.partyA_url
        } else if (party.includes('PartyB')){
            return urls.partyB_url
        } else if (party.includes('PartyC')) {
            return urls.partyC_url
        }
    }

    return (
        <div>
            {/* eslint-disable-next-line */}
            <a type="button"
               className="btn btn-2"
               onClick={changeText(getButtonText())}>{buttonText}</a>
            { shouldDisplayTable &&
            <table className="pa1 tl network-wrapper">
                <tbody>
                {parties.map((party, index) => {
                    return <tr>
                        <td className="pv2" key={index}>
                            <a href={() => getUrlForParty((party))}
                               type="button"
                               onClick={() =>  openApplicationWindow(party) }
                               className="bg-transparent bn f4 white grow">Hello</a>
                        </td>
                    </tr>
                })}
                </tbody>
            </table>}
        </div>
    );
}
export default NetworkInfo;
