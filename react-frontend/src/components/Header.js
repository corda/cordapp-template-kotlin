import React from 'react';
import { NODE_ID } from "../services/urls";


function Header() {

    return (
        <div className="tc w-100 white vh-30 bg-mid-gray tr">
            <span className="f3 lh-copy">Dashboard for </span><span className="f1 lh-copy b">{NODE_ID}</span>
        </div>
    );
}
export default Header;
