function openNav() {
    document.getElementById("mySides").style.width = "300px";
}

function closeNav() {
    document.getElementById("mySides").style.width = "0";
}


var global_notification_counter = 0;
var marker;
var map;
function initialize(){
    var mapProp = {
        center:new google.maps.LatLng(45.4420061,10.9954850),
        zoom:18,
        mapTypeId:google.maps.MapTypeId.ROADMAP
    };
    map =new google.maps.Map(document.getElementById("googleMap"),mapProp);

}

var config = {
    apiKey: "AIzaSyB4ID7G0OMF-plCQjLgZXsP-SrpcWylmao",
    authDomain: "staysafe-2876c.firebaseapp.com",
    databaseURL: "https://staysafe-2876c.firebaseio.com",
    storageBucket: "staysafe-2876c.appspot.com",
  };
firebase.initializeApp(config);

$(document).ready(function(){
	
    $('#open-span').click(function(){
        $('.side-bar').css('width','400');
    });

      $(".main-emergency").click(function(){
        $('.side-bar').width(0);
      });
    
	function updateField(uid){
	
		 var ref = firebase.database().ref();
         ref.child('alert').child(uid).update({'status':'processed'});
         ref.child('alert').child(uid).once('value').then(function(snapshot){
             ref.child('accidents').child(snapshot.val().type).child(snapshot.key).set(snapshot.val())
             ref.child('alert').child(snapshot.key).set(null);
         })
         
    	

}
	
	var myRef = firebase.database().ref().child('alert');
	myRef.on('child_added', function(snapshot){
		
		data = snapshot.val();
		key = snapshot.key;
		

        if(!(data.status == 'processed')){

            global_notification_counter++;
            $("#counter").text(global_notification_counter);
            var className = "hide"
            var selected = $('.selected');
            if(selected === undefined || selected === null || selected.size() === 0 ){
                className = "selected";
                marker = new google.maps.Marker({
                    position: {lat: parseFloat(data.latitude), lng:parseFloat(data.longitude)},
                    map: map,
                    title: 'Hello World!'
                });
                map.panTo(marker.position);
            }

            $('#alert-info').append("<div id='"+ key + "' class='" + className+"' latitude="+data.latitude+" longitude="+data.longitude+">"+
                    "<h3 class='info left'> User Id</h3>"+"<p class='info bold'>"+ key + "</p>"+
                    "<h3 class='info left'> Latitude</h3>"+"<p class='info bold'>"+ data.latitude + "</p>"+
                    "<h3 class='info left'> Longitude</h3>"+"<p class='info bold'>" + data.longitude +"</p>"+
                    "<h3 class='info left'> Phone</h3>"+"<p class='info bold'>"+ data.phone +" </p> "+
                    "<h3 class='info left'> Accident Type</h3>"+"<p class='info bold'>"+"Ciaone"+ data.type +"</p>" +   
                    "<h3 class='info left'> Status</h3>"+"<p class='info bold'>"+ data.status + "</p>" +
                    "<input type=\"button\" class=\"btnRemove\" id=\"" + key + "\" value=\"Send Ambulance\"/><br><br></div>");

            $('input#'+key).click(function(e){
            e.preventDefault();
            var clickedKey = $(this).attr('id');
            global_notification_counter--;
            $("#counter").text(global_notification_counter);
            //alert(location);
            //$("#myList li").eq(0).remove();

            var nextElem=$('div#'+clickedKey).next();
            $('div#'+clickedKey).remove();
            marker.setMap(null);
            updateField(clickedKey);

            if(nextElem === undefined || nextElem === null || nextElem.size() === 0 )
                return;

            nextElem.addClass('selected');
            nextElem.removeClass('hide');
            
            var lat = nextElem.attr('latitude');
            var lon = nextElem.attr('longitude');
            marker = new google.maps.Marker({
                position: {lat: parseFloat(lat), lng:parseFloat(lon)},
                map: map,
                title: 'Hello World!'
            });
            map.panTo(marker.position);


            
            
            
        });
        }
			
        
	});

    


});

