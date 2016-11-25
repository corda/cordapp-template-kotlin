"use strict";

var app = angular.module('demoAppModule', ['ui.bootstrap']);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    var demoApp = this;

    // We identify the node based on its localhost port.
    var nodePort = $location.port();
    var apiBaseURL = "http://localhost:" + nodePort + "/api/example/";
    demoApp.thisNode = '';
    demoApp.pos = [];

    $http.get(apiBaseURL + "who-am-i").then(function(response) {
        demoApp.thisNode = response.data.me;
    });

    demoApp.openModal = function (size) {
        var modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance'
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

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance) {
    var modalInstance = this;

    var nodePort = $location.port();
    var apiBaseURL = "http://localhost:" + nodePort + "/api/example/";
    modalInstance.form = {};
    modalInstance.formError = false;
    modalInstance.peers = [];
    modalInstance.items = [{}];

    $http.get(apiBaseURL + "get-peers").then(function(response) {
        modalInstance.peers = response.data.peers;
    });

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

        var inValidCounterparty = true;
        for (var i = 0; i < modalInstance.peers.length; i++) {
            if (modalInstance.peers[i] === modalInstance.form.counterparty) {
                inValidCounterparty = false;
                break;
            }
        }

        var invalidItemFields = false;
        for (i = 0; i < modalInstance.items.length; i++) {
            var item = modalInstance.items[i];
            if (!item.name || !item.amount || isNaN(item.amount)) {
                invalidItemFields = true;
                break;
            }
        }

        return invalidNonItemFields || inValidCounterparty || invalidItemFields;
    }
});