import React from 'react';
import { NODE_ID } from "../services/urls";

function Header() {

    return (
        <div className="w-100 white vh-30 bg-mid-gray shadow-4 flex">
            <div className="tl w-50 pa3 mr2">
                {/*<img src="crda-logo.svg" width="100%" alt="Corda Logo"/>*/}
            </div>
            <div className="tl w-50 pa3 mr2">
                <span className="f1 lh-copy b tc">{NODE_ID}</span>
            </div>
        </div>
    );
}
export default Header;
