insert into AlarmInterface (INTERFACEID, DESCRIPTION, STATUS, COLLECTION_SET_ID, COLLECTION_ID, QUEUE_NUMBER) values ('AlarmInterface_15min', 'Alarm interface for 15 minutes interval.', 'active', 9, 99, 1);

insert into AlarmInterface (INTERFACEID, DESCRIPTION, STATUS, COLLECTION_SET_ID, COLLECTION_ID, QUEUE_NUMBER) values ('Reduced_delay', 'Alarm interface for reduced delay alarms.', 'active', 99, 999, 1);

insert into AlarmReport (INTERFACEID, REPORTID, REPORTNAME, URL, STATUS) values ('AlarmInterface_15min', '02afd646-cab9-471d-bb02-3e58ae2e3d8d', 'AM_RAN_CCDEVICE_pmSumCcSpMeasLoad', 'reportname=AM_RAN_CCDEVICE_pmSumCcSpMeasLoad', 'ACTIVE');
insert into AlarmReportParameter (REPORTID, NAME, VALUE) values ('02afd646-cab9-471d-bb02-3e58ae2e3d8d', 'eniqBasetableName', 'DC_E_RAN_CCDEVICE_RAW');
insert into AlarmReportParameter (REPORTID, NAME, VALUE) values ('02afd646-cab9-471d-bb02-3e58ae2e3d8d', 'Row Status:', '');

insert into AlarmReport (INTERFACEID, REPORTID, REPORTNAME, URL, STATUS) values ('AlarmInterface_15min', '6306-66-6887366-410511L', 'AM_RAN_CCDEVICE_pmSumCcSpMeasLoadOld', 'reportname=AM_RAN_CCDEVICE_pmSumCcSpMeasLoadOld', 'INACTIVE');
insert into AlarmReportParameter (REPORTID, NAME, VALUE) values ('6306-66-6887366-410511L', 'eniqBasetableName', 'DC_E_RAN_CCDEVICE_RAW');
insert into AlarmReportParameter (REPORTID, NAME, VALUE) values ('6306-66-6887366-410511L', 'Row Status:', '');

insert into AlarmReport (INTERFACEID, REPORTID, REPORTNAME, URL, STATUS) values ('Reduced_delay', '3160c8e0-56fb-47bd-a784-4bc1752917a2', 'AM_RAN_CCDEVICE_pmSumCcSpMeasNoLoad', 'reportname=AM_RAN_CCDEVICE_pmSumCcSpMeasNoLoad', 'ACTIVE');
insert into AlarmReportParameter (REPORTID, NAME, VALUE) values ('3160c8e0-56fb-47bd-a784-4bc1752917a2', 'eniqBasetableName', 'DC_E_RAN_CCDEVICE_RAW');
insert into AlarmReportParameter (REPORTID, NAME, VALUE) values ('3160c8e0-56fb-47bd-a784-4bc1752917a2', 'Row Status:', '');
