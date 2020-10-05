export const NODE_HOST = process.env.REACT_APP_APIHOST.trim();
export const NODE_ID = process.env.REACT_APP_USER_ID.trim();
export const NODE = NODE_HOST.slice(7,22)

export default {
    get_parties: `${NODE_HOST}/parties/`,
    get_flows: `${NODE_HOST}/flows/`,
    get_cordapps: `${NODE_HOST}/cordapps/`,
    start_flow: `${NODE_HOST}/start-flow/`,
    get_network_params: `${NODE_HOST}/network-parameters/`,

    websocket: `ws://${NODE}/vault-events`,

    partyA_url: `http://localhost:3001`,
    partyB_url: `http://localhost:3002`,
    partyC_url: `http://localhost:3003`
}
