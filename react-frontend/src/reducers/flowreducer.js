function flowReducer(state, action) {
    switch (action.type) {
        case "ADD_COMPLETED_FLOW": {
            const newFlow = action.payload.completedFlow;
            return {completedFlows: [newFlow, ...state.completedFlows]}
        }
        default:
            return state;
    }

}

export default flowReducer;