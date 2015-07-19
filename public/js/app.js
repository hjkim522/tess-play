var selectedExpert = undefined;
var selectedExpertIdx = 0;
var selectedRT = undefined;
var selectedRTIdx = 0;

function selectTerm(idx) {
    $("#originRT"+idx).attr("checked", false);
    $("#rt_originRT"+idx).val("");
}

function selectOptExpert() {
    $("#searchRT").attr("checked", false);
    $("#rt_searchRT"+selectedExpertIdx).val("");
    $("#rt_optExpert"+selectedExpertIdx).val("");
}

function openExpert(elem, idx) {
    selectedExpert = $(elem);
    selectedExpertIdx = idx;
    $("#search-term").val("");
    $("#search-result").empty();
    $("#search-result").append('<div class="radio"><label><input type="radio" name="optExpert" value="newTerm" onclick="openRT(this,'+idx+')"><span>Add new terminology to dictionary</span></label></div>');
    $("#search-result").append('<div class="radio"><label><input type="radio" name="optExpert" value="notApplicable"><span>Not to add terminology to dictionary</span></label></div>');
}

function openRT(elem, idx) {
    selectedRT = $(elem);
    selectedRTIdx = idx;
    if (selectedRT.is(':checked') == false)
        return;

    var title = "";
    var targetTerm = "";
    var targetCid = "";
    var term = "";

    // RT for originPhrase
    if (selectedRT.attr("name") == "originRT") {
        var radio = $("input:radio[name=radio"+idx+"]:checked");
        if (radio.length == 0) {
            alert("Please select a proper term before registering to RT");
            selectedRT.attr("checked", false);
            return;
        }

        // handling newTerm case
        //XXX: separate newTerm and rt
        if (radio.val() == "expert") {
            var expert = JSON.parse($("#expert"+idx).val());
            if (expert["action"] != "newTerm") {
                alert("Please select a proper term before registering to RT");
                selectedRT.attr("checked", false);
                return;
            }
            else {
                var newTerm = JSON.parse($("#rt_optExpert"+idx).val());
                targetTerm = newTerm["targetTerm"];
                targetCid = "";
            }
        }

        // normal cases
        else {
            targetTerm = radio.parent().children("span").text();
            targetCid = radio.val();
        }

        title = "Register to dictionary";
        term = selectedRT.val();
    }

    // RT for search
    else if (selectedRT.attr("name") == "searchRT") {
        var radio = $("input:radio[name=optExpert]:checked");
        if (radio.length == 0 || radio.val() == "newTerm" || radio.val() == "notApplicable") {
            alert("Please select a term before registering to RT");
            selectedRT.attr("checked", false);
            return;
        }

        title = "Add retrieved terminology to dictionary";
        targetTerm = radio.parent().children("span").text();
        targetCid = radio.val();
        term = $("#search-term").val();
    }

    // RT for newTerm
    else if (selectedRT.attr("name") == "optExpert") { // new term
        if ($("#search-term").val() == "") {
            alert("Please write a term to register");
            selectedRT.attr("checked", false);
            return;
        }

        title = "Add new terminology to dictionary";
        targetTerm = $("#search-term").val();
        targetCid = "";
        term = $("#search-term").val();
    }

    $("#modalRTLabel").text(title);
    $("#rt-target-term").text(targetTerm);
    $("#rt-target-cid").text(targetCid);
    $("#rt-term").val(term);
    $("#modalRT").modal();
}

$("#rt-confirm").click(function() {
    var rt = {};
    rt["targetTerm"] = $("#rt-target-term").text();
    rt["targetCid"] = $("#rt-target-cid").text();
    rt["term"] = $("#rt-term").val();
    rt["preferred"] = $("#inputPreferred").val();
    rt["expert"] = $("#inputExpertName").val();
    rt["semantic"] = $("#inputSemantic").val();
    rt["language"] = $("#inputLanguage").val();
    console.log(rt);

    $("#rt_" + selectedRT.attr("name") + selectedRTIdx).val(JSON.stringify(rt));
    $("#modalRT").modal('hide');
});

$("#search-btn").click(function() {

    if($("#choice2").prop("checked") )
    {
        var score = "1000";
        $.ajax({
            type: "POST",
            url: "/rt", //"/search",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({ "term": $("#search-term").val() }),
            success: function(response) { // json response
                $("#search-result").empty();
                $("#search-result").append('<div><b>accuracy(0~1000)&nbsp;&nbsp;&nbsp;&nbsp;candidate of standard term</b></div>');
                for(var i in response) {
                    var str = response[i].str;
                    var sty = response[i].sty;
                    var text ="[" + sty + "]";

                    if (str)
                        $("#search-result").append('<div class="radio"><label><input type="radio" name="optExpert" value="'+str+'" onclick="selectOptExpert()"><span>&nbsp;'+score+'&nbsp;&nbsp;&nbsp;&nbsp;'+ str+'&nbsp;'+text+'</span></label></div>');

                }
                if(str){
                    $("#search-result").append('<div class="checkbox" style="margin-top: -5px; padding-bottom: 10px; border-bottom: 1px solid #ddd"><label><input type="checkbox" id="searchRT" name="searchRT" value="RT" onclick="openRT(this,'+selectedExpertIdx+')">Add retrieved terminology to dictionary</label></div>');
                }
                $("#search-result").append('<div class="radio"><label><input type="radio" name="optExpert" value="newTerm" onclick="selectOptExpert();openRT(this,'+selectedExpertIdx+')"><span>Add new terminology to dictionary</span></label></div>');
                $("#search-result").append('<div class="radio"><label><input type="radio" name="optExpert" value="notApplicable" onclick="selectOptExpert()"><span>Not to add terminology to dictionary</span></label></div>');

            },
            error: function(XMLHttpRequest, textStatus, errorThrown) {
                alert("Error: " + errorThrown);
            }
        });
    }
    else
    {

        $.ajax({
            type: "POST",
            url: "/metamap", //"/search",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({ "term": $("#search-term").val() }),
            success: function(response) { // json response
                $("#search-result").empty();
                $("#search-result").append('<div><b>accuracy(0~1000)&nbsp;&nbsp;&nbsp;&nbsp;candidate of standard term</b></div>');
                for(var i in response) {
                    var conceptid = response[i].conceptid;
                    var text = response[i].text;
                    var score = response[i].score;
                    $("#search-result").append('<div class="radio"><label><input type="radio" name="optExpert" value="'+conceptid+'" onclick="selectOptExpert()"><span>'+score+'&nbsp;&nbsp;&nbsp;&nbsp;'+text+'</span></label></div>');
                }
                $("#search-result").append('<div class="checkbox" style="margin-top: -5px; padding-bottom: 10px; border-bottom: 1px solid #ddd"><label><input type="checkbox" id="searchRT" name="searchRT" value="RT" onclick="openRT(this,'+selectedExpertIdx+')">Add retrieved terminology to dictionary</label></div>');
                $("#search-result").append('<div class="radio"><label><input type="radio" name="optExpert" value="newTerm" onclick="selectOptExpert();openRT(this,'+selectedExpertIdx+')"><span>Add new terminology to dictionary</span></label></div>');
                $("#search-result").append('<div class="radio"><label><input type="radio" name="optExpert" value="notApplicable" onclick="selectOptExpert()"><span>Not to add terminology to dictionary</span></label></div>');
            },
            error: function(XMLHttpRequest, textStatus, errorThrown) {
                alert("Error: " + errorThrown);
            }
        });
    }

    return false;
});


$("#search-confirm").click(function() { // expert-confirm
    if (selectedExpert == undefined)
        return;

    var radio = $("input:radio[name=optExpert]:checked");
    var text = radio.parent().children("span").text();

    if (radio.length == 0)
        return;
    if (radio.val() == "newTerm")
        text = text + " (" + $("#search-term").val() + ")";

    selectedExpert.nextAll().remove();
    selectedExpert.parent().append('<span>Expert defined - '+text+'</span>');

    var expert = {};
    expert["action"] = radio.val();
    expert["desc"] = text;
    $("#expert"+selectedExpertIdx).val(JSON.stringify(expert));
});

$("#btn-submit").click(function() {
    var hid = $("#hid").val();
    var data = [];

    // create json object
    var n = $("[id^=originPhrase]").length;
    for (i = 1; i <= n; i++) {
        var result = {}; // radio@i value
        result["term"] = $("#originPhrase"+i).text();

        // get resolved result
        var radio = $("input:radio[name=radio"+i+"]:checked");
        if (radio.length != 1) {
            alert("Not resolved : " + result["term"]);
            return false;
        }

        result["action"] = radio.val();
        result["desc"] = radio.parent().children("span").text();

        // if expert defined
        if (radio.val() == "expert") {
            result["expert"] = JSON.parse($("#expert"+i).val());
            if (result["expert"]["action"] == "newTerm")
                result["rtNew"] = JSON.parse($("#rt_optExpert"+i).val());
            else if (result["expert"]["action"] != "notApplicable" && $("#rt_searchRT"+i).val() != "")
                result["rtSearch"] = JSON.parse($("#rt_searchRT"+i).val());
        }

        if ($("#originRT"+i).is(":checked"))
            result["rtOrigin"] = JSON.parse($("#rt_originRT"+i).val());

        data.push(result);
    }

    // save
    $.ajax({
        type: "POST",
        url: "/terms/" + hid,
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify(data),
        success: function(response) { // json response
            window.location='/terms';
        },
        error: function(XMLHttpRequest, textStatus, errorThrown) {
            alert("Error: " + errorThrown);
        }
    });
    return false;
});

$(function () {
    $('[data-toggle="tooltip"]').tooltip();
});
