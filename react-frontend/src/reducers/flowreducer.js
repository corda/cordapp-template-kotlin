function flowReducer(state, action) {
    switch (action.type) {
        case "ADD_COMPLETED_FLOW": {
            const newFlow = action.payload.completedFlow;
            console.log("ADDING TO REDUCER" + newFlow)
            return {completedFlows: [newFlow, ...state.completedFlows]}
        }
        // case "DELETE_POST": {
        //     const postId = action.payload.id;
        //     return { posts: state.posts.filter( post => post.id !== postId) }
        // }
        default:
            return state;
    }

}

export default flowReducer;