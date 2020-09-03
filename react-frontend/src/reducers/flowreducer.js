function flowReducer(state, action) {
    switch(action.type) {
        case "LOAD_FLOW_PARAMS": {
            return {
                ...state,
                flowParams: action.data,
                flowMessage: "",
                messageType: true
            }
        }
        case "LOAD_PARTIES":
            return {
                ...state,
                parties: action.payload
            }
        case "UPDATE_PARAM_VAL":
            return{
                ...state,
                flowParams: action.data
            }
        case "CLOSE_TX_MODAL":
            return{
                ...state,
                showTxPopup: false,
                isFlowSelected: false,
                flowMessage: "",
                messageType: true
            }
        case "OPEN_TX_MODAL":
            return{
                ...state,
                showTxPopup: true
            }

        default:
            return state;
    }
}

export default flowReducer;