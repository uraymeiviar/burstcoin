/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.rewardRecipient = "";

	NRS.pages.mining = function() {
		if(NRS.rewardRecipient === ""){
			NRS.rewardRecipient = NRS.account;
		}
		NRS.sendRequest("getRewardRecipient", {
			"account": NRS.account
		}, function(response) {
			NRS.rewardRecipient = response.rewardRecipient;
			var nxtAddress = new NxtAddress();
			nxtAddress.set(NRS.rewardRecipient);

			$('#mining_reward_recipient_account').html(nxtAddress.toString());
			$('#mining_reward_recipient_account').attr('data-user',nxtAddress.toString());
		});	
	}

	NRS.updateMiningState = function() {
		NRS.sendRequest("getMiningState", {
		}, function(response) {
			var rows = "";
			if(response.hasOwnProperty('plots')) {
				rows += '<tr>';
				for(var plot in response.plots) {
					var accountRS = NRS.convertNumericToRSAccountFormat(response.plots[plot].accountId);
					var fullSize = response.plots[plot].nonceCount / 4;
					var fill = 100*response.plots[plot].sizeMB/fullSize;
					rows += "<td><a href='#' class=\"fixedWidthFont\" data-user='" + NRS.getAccountFormatted(accountRS, "account") + "' class='user_info'>" + NRS.getAccountTitle(accountRS, "account") + "</a></td>"
					rows += '<td>'+response.plots[plot].startNonce+'</td>';
					rows += '<td>'+response.plots[plot].nonceCount+'</td>';
					rows += '<td>'+response.plots[plot].stagger+'</td>';
					rows += '<td>'+response.plots[plot].sizeMB+'</td>';
					rows += '<td>'+fill.toFixed(2)+'</td>';
				}
				rows += '</tr>';
			}
			NRS.dataLoaded(rows);
		});	
	}

	$('#stop_mining_button').hide();

	$('#start_mining_button').click( function(e) {
		e.preventDefault();
		if(NRS.miningType == "pool_mining") {
			$('#miningPoolForm').hide();
			var host = $('#mining_host').val();
			var port = $('#mining_host_port').val();
			$('#start_mining_button').attr('disabled','disabled');
			NRS.sendRequest("startMining", {
					host:host,
					port:port
				}, function(response) {
					var result = false;
					if(response.hasOwnProperty('result')) {
						result = response.result;
					}
					if(result === true){
						$('#stop_mining_button').show();
						$('#start_mining_button').hide();	
					}
					$('#start_mining_button').removeAttr('disabled');
				}
			);	
		}
		else {
			
		}
	});

	$("#mining_page_type .btn").click(function(e) {
		//	$("#blocks_page_type li a").click(function(e) {
		e.preventDefault();

		NRS.miningType = $(this).data("type");

		if(NRS.miningType == "pool_mining") {
			$('#miningPoolForm').show();
		}
		else {
			$('#miningPoolForm').hide();
		}
	});

	setInterval(function(){NRS.updateMiningState()},5000);
	return NRS;
}(NRS || {}, jQuery));