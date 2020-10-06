import React, {useState} from 'react';
import ReactDOM from 'react-dom';
import '../styling/Modal.css';
import InnerModal from "./InnerModal";

function Modal ({ registeredFlow, isShowing, toggle }) {
    return isShowing ? ReactDOM.createPortal(
        <React.Fragment>
            <div className="modal-overlay avenir"/>
            <div className="modal-wrapper" aria-modal aria-hidden tabIndex={-1} role="dialog">
                <InnerModal registeredFlow={registeredFlow} toggle={toggle} outsideClickIgnoreClass={'MuiMenuItem-root'}/> {/*Allows the modal to be closed by clicking outside of the main window */}
            </div>
        </React.Fragment>, document.body
    ) : null;
}

export default Modal;