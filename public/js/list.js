var skip = 15;

$("#btn-more").click(function() {
    $.ajax({
        type: "GET",
        url: "/terms?offset="+skip,
        success: function(response) { // json response
            for(var i in response) {
                var hid = response[i].hid;
                var context = response[i].context;
                var hname = response[i].hname;
                var originDB = response[i].originDB;
                //$("#list-more").append('<div class="row" style="border-bottom: 1px solid #eee; padding: 5px; margin: 0px"><div class="col-xs-2" style="padding-top: 4px">'+hid+'</div><div class="col-xs-8 trim" style="padding-top: 4px">'+context+'</div><div class="col-xs-2" align="right"><a href="/terms/'+hid+'" class="btn btn-info btn-sm">Resolve</a></div></div>');
                $("#list-more").append('<div class="row" style="border-bottom: 1px solid #eee; padding: 5px; margin: 0px"><div class="col-xs-2" style="padding-top: 4px"><a href="http://dbpony.kaist.ac.kr/kotmid/detail.php?database=herb&id='+hid+'">'+hname+'</a></div><div class="col-xs-8 trim" style="padding-top: 4px">'+context+'</div><div class="col-xs-1" style="padding-top: 4px">'+originDB+'</div><div class="col-xs-1" align="right"><a href="/terms/'+hid+'" class="btn btn-info btn-sm">Resolve</a></div></div>');
            }
            skip = skip + 10;
        },
        error: function(XMLHttpRequest, textStatus, errorThrown) {
            alert("Error: " + errorThrown);
        }
    });

    return false;
});
