import React from 'react';
import { NODE_ID } from "../services/urls"
import '../styling/Header.css';

export const formatHeader = (name) => {
    switch(name) {
        case 'PartyA':
            return 'Party A 🇬🇧';
        case 'PartyB':
            return 'Party B 🇦🇺';
        case 'PartyC':
            return 'Party C 🇺🇸';
        default:
            return 'foo';
    }
}

function Header() {

    return (
        <div className="Header">
            {/*<div className="tl w-50 pa1 mr2">*/}
            {/*    /!*<img src="crda-logo.svg" width="100%" alt="Corda Logo"/>*!/*/}
            {/*</div>*/}
            <div className="tc">
                <span className="f1 lh-copy b tc">{formatHeader(NODE_ID)}</span>
            </div>
        </div>
    );
}
export default Header;
