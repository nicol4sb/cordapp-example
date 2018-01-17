"use strict";

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('NdaDashboardController', function($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "/api/example/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    demoApp.getNdaRequests = () => $http.get(apiBaseURL + "getNdaRequests")
        .then((response) => demoApp.ndaRequest = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getNdaRequests();

     demoApp.openModal = (request) => {
            const modalInstance = $uibModal.open({
                templateUrl: 'demoAppModal.html',
                controller: 'ModalInstanceCtrl',
                controllerAs: 'modalInstance',
                resolve: {
                    request: () => request,
                    apiBaseURL: () => apiBaseURL,
                }
            });

            modalInstance.result.then(() => {}, () => {});
        };
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, request, apiBaseURL) {
    const modalInstance = this;

    modalInstance.request = request;
    modalInstance.form = {};
    modalInstance.formError = false;
    
    // Validate and create IOU.
    modalInstance.updatedaRequest = () => {
           
            $uibModalInstance.close();

            const createIOUEndpoint = `${apiBaseURL}review-nda?ndaPreviousStateId=${request.linearId.id}&ndaRequestText=${modalInstance.form.value}`;

            // Create PO and handle success / fail responses.
            $http.put(createIOUEndpoint).then(
                (result) => {l
                    modalInstance.displayMessage(result);
                    demoApp.getNdaRequests();
                },
                (result) => {
                    modalInstance.displayMessage(result);
                }
            );
        
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

    // Close create IOU modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        return (modalInstance.form.counterparty === undefined);
    }
});

