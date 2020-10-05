import React from 'react';
import onClickOutside from "react-onclickoutside";
import FlowParameters from "./FlowParameters";

function InnerModal({registeredFlow, toggle}) {
    InnerModal.handleClickOutside = () => toggle()

    return (
        <div className="modal">
            <div className="modal-header" />
            <h3>{trimFlowsForDisplay(registeredFlow.flowName)}</h3>
            <FlowParameters registeredFlow={registeredFlow}/>
        </div>
    );
}

const trimFlowsForDisplay = (text) => {
    let words = text.split(".")
    return words[words.length - 1]
}

const clickOutsideConfig = {
    handleClickOutside: () => InnerModal.handleClickOutside
};

export default onClickOutside(InnerModal, clickOutsideConfig);
