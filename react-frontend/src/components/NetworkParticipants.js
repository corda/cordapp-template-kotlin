import { useState } from 'react';
import React from 'react';
import urls from "../services/urls";
import http from "../services/http";
import '../styling/Button.scss';
import '../styling/NetworkParticipants.css';
import { NODE_ID } from "../services/urls";
import { SHOW_NETWORK_PARTICIPANTS, HIDE_NETWORK_PARTICIPANTS} from "../services/buttons";

export const transformPartyName = (party) => {
    const partyName = party.split('O=')[1].split(',')[0];
    const countryCode = party.split('C=')[1]
    return partyName + getFlag(countryCode)
}

function getFlag(countryCode) {
    switch(countryCode) {
        case 'GB':
            return ' 🇬🇧';
        case 'AU':
            return ' 🇦🇺';
        case 'US':
            return ' 🇺🇸';
        case 'FR':
            return ' 🇫🇷';
        default:
            return '';
    }
}

function NetworkParticipants() {
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
                <table className="pa1 tl network-wrapper">
                    <tbody>
                    {parties.map((party, index) => {
                        return <tr>
                            <td className="pv2" key={index}>
                                <a href={() => getUrlForParty((party))}
                                   type="button"
                                   onClick={() =>  openApplicationWindow(party) }
                                   className="bg-transparent bn f4 white grow">{transformPartyName(party)}</a>
                            </td>
                        </tr>
                    })}
                    </tbody>
                </table>}
        </div>
    );
}
export default NetworkParticipants;
