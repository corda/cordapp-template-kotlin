"use strict";

// --------
// WARNING:
// --------

// THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE! DO NOT USE IN PRODUCTION!

// FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS NOT THE RECOMMENDED WAY TO INTERFACE
// WITH CORDA NODES! HOWEVER, FOR THIS PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
// YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.

// GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES VIA THE NODE'S RPC INTERFACE. IN THE
// COMING WEEKS WE'LL WRITE A TUTORIAL ON HOW BEST TO DO THIS.

var app = angular.module('demoAppModule', ['ui.bootstrap']);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    var demoApp = this;

    // We identify the node based on its localhost port.
    var nodePort = $location.port();
    var apiBaseURL = "http://localhost:" + nodePort + "/api/example/";
    demoApp.thisNode = '';
    var peers = [];
    demoApp.pos = [];

    $http.get(apiBaseURL + "who-am-i").then(function(response) {
        demoApp.thisNode = response.data.me;
    });

    $http.get(apiBaseURL + "get-peers").then(function(response) {
        peers = response.data.peers;
    });

    demoApp.openModal = function (size) {
        var modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                nodePort: function() {
                    return nodePort;
                },
                apiBaseURL: function() {
                    return apiBaseURL;
                }, 
                peers: function() {
                    return peers;
                }
            }
        });

        modalInstance.result.then(function () {
            // Ignore modal close.
        }, function () {
            // Ignore modal dismissal.
        });
    };

    demoApp.getPOs = function() {
        $http.get(apiBaseURL + "purchase-orders").then(function(response) {
            var newPos = [];
            for (var tx in response.data) {
                var po = response.data[tx].state.data;
                newPos.push(po);
            }

            newPos.reverse();
            demoApp.pos = newPos;
        });
    };

    demoApp.getPOs();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, nodePort, apiBaseURL, peers) {
    var modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;
    modalInstance.items = [{}];

    modalInstance.create = function () {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            var po = {
                orderNumber: modalInstance.form.orderNumber,
                deliveryDate: modalInstance.form.deliveryDate,
                deliveryAddress: {
                    city: modalInstance.form.city,
                    country: modalInstance.form.country
                },
                items: modalInstance.items
            };

            $uibModalInstance.close();

            var createPoEndpoint = apiBaseURL + modalInstance.form.counterparty + "/create-purchase-order";
            $http.put(createPoEndpoint, angular.toJson(po));
        }
    };

    modalInstance.cancel = function () {
        $uibModalInstance.dismiss();
    };

    modalInstance.addItem = function() {
        modalInstance.items.push({});
    };

    modalInstance.deleteItem = function() {
        modalInstance.items.pop();
    };

    function invalidFormInput() {
        var invalidNonItemFields = !modalInstance.form.orderNumber
            || isNaN(modalInstance.form.orderNumber)
            || !modalInstance.form.deliveryDate
            || !modalInstance.form.city
            || !modalInstance.form.country;

        var inValidCounterparty = modalInstance.form.counterparty === undefined;

        var invalidItemFields = false;
        for (var i = 0; i < modalInstance.items.length; i++) {
            var item = modalInstance.items[i];
            if (!item.name || !item.amount || isNaN(item.amount)) {
                invalidItemFields = true;
                break;
            }
        }

        return invalidNonItemFields || inValidCounterparty || invalidItemFields;
    }
});