alter table restricted_patients drop column id;
alter table restricted_patients add primary key  (prisoner_number);