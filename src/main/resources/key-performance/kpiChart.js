

/**
 * Build HTML table from data array.
 * @param container the parent element for the table
 * @param kpiCollection the data array
 */
var buildKpiTable =  function (container, kpiCollection){

	container.append("<table id ='kpi-table'/>");
	AJS.$('<tr>').attr('id', 'table-header').appendTo('#kpi-table');
	AJS.$('#table-header').append('<th>Projekt</th>');
	AJS.$('#table-header').append('<th>KPI</th>');
	AJS.$('#table-header').append('<th>Count</th>');
	AJS.$('#kpi-table').append('</tr>');
	var i = 0;
	kpiCollection.each(function()
            {
		i++;
				AJS.$('<tr/>').attr('id','row'+i).appendTo("#kpi-table");
				AJS.$('#row'+i).append('<td>'+this.projectKey+'</td>');
				AJS.$('#row'+i).append('<td>'+this.kpiNumber+'</td>');
				AJS.$('#row'+i).append('<td>'+this.issueCount+'</td>');

            });
};

/**
 * Converts a JSON representation into an array.
 * @param kpiCollection the kpiCollection in JSON format
 * @returns the array
 */
var convertJsonToArray = function (kpiCollection){



	var kpiArray = new Array();
	var counter= 0;
	kpiCollection.each(function () {

		var array = new Array();
		array[0] = this.Time;
		var projects = this.ProjectKPI;
		for(i=0; i<projects.length; i++){
			array[i+1] = projects[i].KpiNumber;
		}
		kpiArray[counter] = array;
		counter++;
	});
	return kpiArray;

};


/**
 * This function represents the gadgets view and config screen.
 * It uses the Atlassian Javascript SDK and refers to googles visualization api for drawing the graph.
 * @param baseUrl url of jira instance
 * @param title the title of the gadget
 * @param subTitle the subtitle of the gadget.
 * @returns the gadget
 */
function buildKpiGadget(baseUrl, title, subTitle) {
	return AJS.Gadget({
                    baseUrl: baseUrl,
                    useOauth: "/rest/gadget/1.0/currentUser",
                    config: {
                        descriptor: function(args)
                        {

                            var gadget = this;
                            gadgets.window.setTitle(title);

                            var projectPicker = AJS.gadget.fields.projectsOrCategoriesPicker(gadget, "projectId", args.projectOptions);
                            var periodPicker = AJS.gadget.fields.days(gadget, "period");
                            var intervalPicker = {
                            	userpref: "interval",
                            	label: "Interval steps",
                            	description: "How many interval steps should be shown for the specified period?",
                            	type: "select",
                            	selected: gadget.getPref("interval"),
                            	options:[
                            	         {
                            	        	 label: "daily",
                           	        		 value: "daily"
                            	         },
                            	         {
                            	        	 label: "weekly",
                           	        		 value: "weekly"
                            	         },
                            	         {
                            	        	 label: "monthly",
                           	        		 value: "monthly"
                            	         }
                            	         ]


                            };
                            return {
                            	action: "/rest/key-performance/1.0/key-performance/validate",
                                theme : function()
                                {
                                    if (gadgets.window.getViewportDimensions().width < 450)
                                    {
                                        return "gdt top-label";
                                    }
                                    else
                                    {
                                        return "gdt";
                                    }
                                }(),
                                fields: [
                                    projectPicker,
                                    periodPicker,
                                    intervalPicker,
                                    AJS.gadget.fields.nowConfigured()
                                ]
                            };
                        },
                        args: function()
                        {
                            return [
                                {
                                    key: "projectOptions",
                                    ajaxOptions: "/rest/gadget/1.0/projectsAndProjectCategories"
//                                    ajaxOptions:  "/rest/gadget/1.0/filtersAndProjects?showFilters=false"
                                },
                            ];
                        }()
                    },
                    view: {
                        onResizeAdjustHeight: true,
                        enableReload: true,
                        template: function (args)
                        {

                    	 var gadget = this;
                         gadget.getView().empty();
                         gadget.projectOrFilterName = "here is the test project or filter name";

                         var projectsSelected = gadget.getPref('projectSelector');

                         var container = AJS.$("<div id='chart_div'/>");
                         gadget.getView().append(container);

                         if(args.kpiCollection != null){

                        	 var kpiCollection;
                        	 kpiCollection = AJS.$(args.kpiCollection.KpisAtTime);

	                         if(kpiCollection != null){
		                         var data = new google.visualization.DataTable();
		                         // Declare columns and rows.
		                         data.addColumn('string', 'Date');  // Column 0 is the x-axis, so for us it is the date

		                         var sampleProjectsForInit = kpiCollection[0].ProjectKPI;
		                         for(i = 0; i < sampleProjectsForInit.length; i++){
		                        	 data.addColumn('number', sampleProjectsForInit[i].projectKey);
		                         }
		                         //all further columns represenet one date
		                         //so for one date, the kpi is given for alle filtered projects
		                         data.addRows(convertJsonToArray(kpiCollection));

		                         var width = gadgets.window.getViewportDimensions().width - 10;
		                         var height = width * 2/3;
		                         var chart = new google.visualization.LineChart(document.getElementById('chart_div'));
                                chart.draw(data, {width: width, height: height,title: title});
//                                chart.draw(data, {width: width, height: height, is3D: true,
//                                legend:this.getPref("legend"), title: this.getMsg("gadget.user.activity.chart.title")});
	                         }else {
	                        	 gadget.getView().append("<p>No Data available</p>");
	                         }
                         }else {
	                    	   gadget.getView().append("<p>No Data available</p>");
                         }

                         gadget.resize();

                        },
                        args: [
                            {
                            	key: "kpiCollection",
                            	ajaxOptions: function()
                            	{
                            		return{
                            			url: "/rest/key-performance/1.0/key-performance/getKpis",
                            			data: {
                            				projectId : gadgets.util.unescapeString(this.getPref("projectId")),
                            				period: this.getPref("period"),
                            				interval: this.getPref("interval"),
                            				end : "today"
                            			}
                            		}
                            	}

                            }
                        ]

                    }
                });
            }


