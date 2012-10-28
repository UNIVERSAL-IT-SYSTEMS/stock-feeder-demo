// UI Feedback that a price has updated
ko.bindingHandlers.flashUpdate = {
    update: function(element) {
    	$(element).fadeOut(100).fadeIn(100);
    }
};

function AppViewModel() {
    var self = this;
    
    // Ensure stock id's are mapped
	var mapping = {
        key: function(data) {
            return ko.utils.unwrapObservable(data.id);
	    }
	}
	
    self.serverError = ko.observable(false);
	self.retrying = ko.observable(false);
	self.serverErrorRetryCountdown = ko.observable(10);
    self.stocks = ko.mapping.fromJS([], mapping);
    self.retyInterval = null;
    
    self.serverError.subscribe(function(error) {
    	if(error) {
    		self.serverErrorRetryCountdown(10);
    		self.retryInterval = setInterval(decreaseErrorCounter, 1000);
    	}
    	else {
    		clearInterval(self.retryInterval);
    	}
    	self.retrying(false);
    });
    
    self.retrying.subscribe(function(retrying) {
    	if(retrying) {
    		self.serverErrorRetryCountdown(0);
    		clearInterval(self.retryInterval);
    	}
    });
    
    function decreaseErrorCounter() {
    	self.serverErrorRetryCountdown(appView.serverErrorRetryCountdown()-1);
    }
}

var appView = new AppViewModel();
ko.applyBindings(appView);

function view() {
	appView.serverError(false);
	loadStream("/view.json");
}

function retry() {
	appView.retrying(true);
	// Retry randomly within 5 seconds
	setTimeout(view, Math.floor(Math.random()*5000));
}

function pollStream() {
	loadStream("/pollStream.json");
}

function loadStream(url) {
	$.getJSON(url, function(data) {
		// Ensure this is an array before doing anything, it could be an empty (0) chunk
		if($.isArray(data)) {
    		// Update data
    		ko.mapping.fromJS(data, appView.stocks);
		}
	}).success(function(){
		appView.serverError(false);
		pollStream();
	}).error(function(){
		appView.serverError(true);
		// Try again in a few seconds
		setTimeout(retry, 10000);
	});	    	
}

$(document).ready(function() {
	// Render view initially
	// Android browser 2.3 seems to need a magical timeout of 200ms so it thinks page load is complete 
	setTimeout(view, 200);
});