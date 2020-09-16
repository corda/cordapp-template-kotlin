import React from 'react';
import { NODE_ID } from "../services/urls"
import '../styling/Header.css';

function Header() {

    return (
        <div className="Header">
            {/*<div className="tl w-50 pa1 mr2">*/}
            {/*    /!*<img src="crda-logo.svg" width="100%" alt="Corda Logo"/>*!/*/}
            {/*</div>*/}
            <div className="tc">
                <span className="f1 lh-copy b tc">{NODE_ID}</span>
            </div>
        </div>
    );
}
export default Header;
