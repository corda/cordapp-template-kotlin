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
