import React from 'react';
import ReactDOM from 'react-dom';
import '../styling/Modal.css';
import FlowParameters from "./FlowParameters";

export const trimFlowsForDisplay = (text) => {
    var words = text.split(".")
    return words[words.length - 1]
}

const Modal = ({ registeredFlow, isShowing, hide }) => isShowing ? ReactDOM.createPortal(

    <React.Fragment>
        <div className="modal-overlay avenir"/>
        <div className="modal-wrapper" aria-modal aria-hidden tabIndex={-1} role="dialog">
            <div className="modal">
                <div className="modal-header">
                    <button type="button" className="modal-close-button bg-transparent" data-dismiss="modal" aria-label="Close" onClick={hide}>
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <h3>{trimFlowsForDisplay(registeredFlow.flowName)}</h3>
                <FlowParameters registeredFlow={registeredFlow}/>
            </div>
        </div>
    </React.Fragment>, document.body
) : null;

export default Modal;