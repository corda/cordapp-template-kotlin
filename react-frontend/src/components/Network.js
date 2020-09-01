import { useState } from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';
import { NODE_ID } from "../services/urls";
import { SHOW_NETWORK_PARTICIPANTS, HIDE_NETWORK_PARTICIPANTS} from "../services/buttons";


function Network() {
    const [parties, setParties] = useState([])
    const [buttonText, setButtonText] = useState("Network Participants")
    const [shouldDisplayTable, setDisplayTable] = useState(false)

    const changeText = (text) => {
        setButtonText(text)
        setDisplayTable(!shouldDisplayTable)
    }

    const getButtonText = () => shouldDisplayTable ? SHOW_NETWORK_PARTICIPANTS : HIDE_NETWORK_PARTICIPANTS

    function openApplicationWindow(party) {
        window.open(getUrlForParty(party), "_blank")
    }

    function getParties() {
        console.log("Getting parties");
        http.get(urls.get_parties)
            .then(r => {
                if(r.status === 200 && r.data.status === true){
                const filteredParties = r.data.data.filter ( party => !party.includes(NODE_ID) && !party.includes("Notary"))
                console.log("parties:" + filteredParties)
                    setParties(filteredParties)
                } else {
                }
            });
        changeText(getButtonText())
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
               onClick={getParties}>{buttonText}</a>
            { shouldDisplayTable &&
                <table className="pa4 tl">
                    <tbody>
                    {parties.map((party, index) => {
                        return <tr>

                            <td className="pv2" key={index}>
                                {/* eslint-disable-next-line */}
                                <a type="button"
                                    onClick={() =>  openApplicationWindow(party) }
                                    className="bg-transparent bn f4 white grow">{party}</a>
                            </td>
                        </tr>
                    })}
                    </tbody>
                </table>}
        </div>
    );
}
export default Network;
