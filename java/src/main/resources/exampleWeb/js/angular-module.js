"use strict";

// --------
// WARNING:
// --------

// THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE!
// DO NOT USE IN PRODUCTION!

// FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS
// NOT THE RECOMMENDED WAY TO INTERFACE WITH CORDA NODES! HOWEVER, FOR THIS
// PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
// YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.

// GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES
// VIA THE NODE'S RPC INTERFACE. IN THE COMING WEEKS WE'LL WRITE A TUTORIAL ON
// HOW BEST TO DO THIS.

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node based on its localhost port.
    const nodePort = $location.port();
    const apiBaseURL = "http://localhost:" + nodePort + "/api/example/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.getPOs = () => $http.get(apiBaseURL + "purchase-orders")
        .then((response) => demoApp.pos = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getPOs();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;
    modalInstance.items = [{}];

    // Validate and create purchase order.
    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            const po = {
                orderNumber: modalInstance.form.orderNumber,
                deliveryDate: modalInstance.form.deliveryDate,
                deliveryAddress: {
                    city: modalInstance.form.city,
                    country: modalInstance.form.country.toUpperCase()
                },
                items: modalInstance.items
            };

            $uibModalInstance.close();

            const createPoEndpoint =
                apiBaseURL +
                modalInstance.form.counterparty +
                "/create-purchase-order";

            // Create PO and handle success / fail responses.
            $http.put(createPoEndpoint, angular.toJson(po)).then(
                (result) => modalInstance.displayMessage(result),
                (result) => modalInstance.displayMessage(result)
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create purchase order modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Add an extra set of item fields.
    modalInstance.addItem = () => modalInstance.items.push({});

    // Remove a set of item fields.
    modalInstance.deleteItem = () => modalInstance.items.pop();

    // Validate the purchase order.
    function invalidFormInput() {
        const invalidNonItemFields = !modalInstance.form.orderNumber
            || isNaN(modalInstance.form.orderNumber)
            || !modalInstance.form.deliveryDate
            || !modalInstance.form.city
            || !modalInstance.form.country;

        const inValidCounterparty = modalInstance.form.counterparty === undefined;

        const invalidItemFields = modalInstance.items
            .map(item => !item.name || !item.amount || isNaN(item.amount))
            .reduce((prev, curr) => prev && curr);

        return invalidNonItemFields || inValidCounterparty || invalidItemFields;
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});
