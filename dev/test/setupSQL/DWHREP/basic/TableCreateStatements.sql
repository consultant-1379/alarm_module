--/***************************
--  AlarmInterface
--/***************************
create table AlarmInterface (
	INTERFACEID varchar(50) not null,
	DESCRIPTION varchar(200) not null,
	STATUS varchar(20) not null,
	COLLECTION_SET_ID numeric(31) not null,
	COLLECTION_ID numeric(31) not null,
	QUEUE_NUMBER numeric(31) not null
);

 alter table AlarmInterface
       add primary key (INTERFACEID);


--/***************************
--  AlarmReport
--/***************************
create table AlarmReport (
	INTERFACEID varchar(50) not null,
	REPORTID varchar(255) not null,
	REPORTNAME varchar(255) not null,
	URL varchar(32000) not null,
	STATUS varchar(10) not null,
	SIMULTANEOUS int null	
);

 alter table AlarmReport
       add primary key (REPORTID);


--/***************************
--  AlarmReportParameter
--/***************************
create table AlarmReportParameter (
	REPORTID varchar(255) not null,
	NAME varchar(255) not null,
	VALUE varchar(255) not null
);

 alter table AlarmReportParameter
       add primary key (REPORTID, NAME);


