"use strict";

angular.module('demoAppModule', [])
  .controller('DemoAppController', function($http) {
    var demoApp = this;

    // TODO: change this to location.port
    var nodePort = 10005;
    var apiBaseURL = "http://localhost:" + nodePort + "/api/example/";

    demoApp.items = [{}];

    $http.get(apiBaseURL + "get-peers").then(function(response) {
        demoApp.peers = response.data.peers;
    });

    $http.get(apiBaseURL + "who-am-i").then(function(response) {
        demoApp.thisNode = response.data.me;
    });

    demoApp.submitPO = function() {
        var po = {
            orderNumber: demoApp.form.orderNumber,
            deliveryDate: demoApp.form.deliveryDate,
            deliveryAddress: {
                city: demoApp.form.city,
                country: demoApp.form.country
            },
            items: demoApp.items
        };

        console.log(po)

        var createPoEndpoint = apiBaseURL + demoApp.form.counterparty + "/create-purchase-order";
        $http.put(createPoEndpoint, angular.toJson(po)).then(function(response) {
            // Refresh the purchase-order list.
            demoApp.getPOs();
            // Clear the form.
            demoApp.form = null;
            demoApp.items = [{}];
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

    demoApp.addItem = function() {
        demoApp.items.push({});
    };

    demoApp.deleteItem = function() {
        demoApp.items.pop();
    };

    var pos = demoApp.getPOs();
  });