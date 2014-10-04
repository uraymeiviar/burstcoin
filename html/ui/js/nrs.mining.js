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

	return NRS;
}(NRS || {}, jQuery));