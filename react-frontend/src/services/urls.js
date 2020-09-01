export const NODE_HOST = process.env.REACT_APP_APIHOST.trim();
export const NODE_ID = process.env.REACT_APP_USER_ID.trim();

export default {
    get_parties: `${NODE_HOST}/parties/`,
    get_flows: `${NODE_HOST}/flows/`,
    get_cordapps: `${NODE_HOST}/cordapps/`,

    choose_category: `${NODE_HOST}chooseCategory`,
    websocket: `ws://${process.env.REACT_APP_WEB_HOST}/vaultEvents`
}