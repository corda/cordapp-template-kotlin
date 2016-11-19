"use strict";

/**
 * --------
 * WARNING:
 * --------
 *
 * THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE! DO NOT USE IN PRODUCTION!
 *
 * FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS NOT THE RECOMMENDED WAY TO INTERFACE
 * WITH CORDA NODES! HOWEVER, FOR THIS PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
 * YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.
 *
 * GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES VIA THE NODE'S RPC INTERFACE. IN THE
 * COMING WEEKS WE'LL WRITE A TUTORIAL ON HOW BEST TO DO THIS.
 */

/**
 * A bit of global state to keep track of the current node being used and other nodes on the network.
 */
var state = {
    "pos": Array(),
    "port": location.port
};

// Render an individual purchase order.
function renderPurchaseOrder(po) {
    // TODO: XSS escaping.
    return '<div class="panel panel-default">' +
        '<div class="panel-body">' +
        '<p>Purchase Order ID: ' + po.po.orderNumber + '</p>' +
        '<p>Linear ID: ' + po.linearId.id + '</p>' +
        '<p>Contract hash: ' + po.contract.legalContractReference + '</p>' +
        '<p>Buyer: ' + po.buyer + '</p>' +
        '<p>Seller: ' + po.seller + '</p>' +
        '<p>Delivery city: ' + po.po.deliveryAddress.city + '</p>' +
        '<p>Delivery Address: ' + po.po.deliveryAddress.country + '</p>' +
        '</div>' +
        '</div>';
}

// Render all agreements for this node.
function renderAgreements() {
    console.log(state.pos.length)
    if (state.pos.length > 0) {
        $('#noAgreements').hide();
        $('#agreements').empty();
        state.pos.forEach(function(po) {
            $('#agreements').append(renderPurchaseOrder(po));
        });
    } else {
        $('#noAgreements').show();
        console.log("There are no purchase orders.");
    }
}

// Grab agreements from the node.
function populateAgreements() {
    // TODO: XSRF.
    $.ajax({
        headers: {'Accept': 'application/json', 'Content-Type': 'application/json'},
        url: "http://localhost:" + state.port + "/api/example/purchase-orders",
        type: "GET",
        dataType: "json",
        success: (result) => {
        state.pos = [];
    Object.keys(result).reverse().forEach((txHash) => {
        const po = result[txHash].state.data;
    state.pos.push(po);
});
    renderAgreements();
}
});
}

$(() => {
    // On page load...
    $(document).ready(function() {
    // Populate agreements if there are any.
    populateAgreements();
    // Build our base url.
    state.url = "http://localhost:" + state.port + "/api/example";

    // Get current node name.
    // TODO: XSRF.
    $.ajax({
        headers: {'Accept': 'application/json', 'Content-Type': 'application/json'},
        url: state.url + "/who-am-i",
        type: "GET",
        dataType: "json",
        success: (result) => {
            state.currentNode = result.me;
            $("#currentNode").text(state.currentNode);
        }
    });

    // Populate counterparty dropdown menu.
    // TODO: XSRF.
    $.ajax({
        headers: {'Accept': 'application/json', 'Content-Type': 'application/json'},
        url: state.url + "/get-peers",
        type: "GET",
        dataType: "json",
        success: (result) => {
            state.peers = result.peers.filter((peer) => { return peer != state.currentNode });
            state.peers.forEach(function(peer) {
                $("#counterparty").append('<option>' + peer + '</option>');
            });
        }
    });
});

// Send transaction.
$("#send").click(() => {
    const data = Object();
    data.orderNumber = $("#orderNumber").val();
    data.deliveryDate = $("#deliveryDate").val();
    data.deliveryAddress = Object();
    data.deliveryAddress.city = $("#city").val();
    data.deliveryAddress.country = $("#country").val();
    data.items = Array();
    const item = Object();
    item.name = $("#itemName").val();
    item.amount = $("#itemAmount").val();
    data.items.push(item);
    console.log(data);
    const counterparty = $("#counterparty").val();
    // TODO: XSRF.
    $.ajax({
            headers: {'Accept': 'application/json', 'Content-Type': 'application/json'},
            url: state.url + "/" + counterparty + "/create-purchase-order",
            type: "PUT",
            dataType: "json",
            data: JSON.stringify(data),
            complete: (result) => { $('#exampleModal').modal('toggle'); }
        });
    });
    // Handle refreshing of agreements.
    $("#refresh").click(() => populateAgreements());
});