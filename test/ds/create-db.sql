-- Copyright 2001-2005 The Apache Software Foundation.
--
-- Licensed under the Apache License, Version 2.0 (the "License")
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

drop table velocity_template if exists;

create table velocity_template (
	id VARCHAR(64) not null,
	timestamp TIMESTAMP,
	def VARCHAR(255) not null
);

insert into velocity_template  (id, timestamp, def) VALUES
	( 'testTemplate1', NOW(), 'I am a test through the data loader');

insert into velocity_template  (id, timestamp, def) VALUES
	( 'testTemplate2', NOW(), '$tool.message $tool.add(23, 19)');

insert into velocity_template  (id, def) VALUES
	( 'testTemplate3', 'This is a template with a null timestamp');

insert into velocity_template  (id, timestamp, def) VALUES
	( 'testTemplate4', NOW(), '#testMacro("foo")');

insert into velocity_template  (id, timestamp, def) VALUES
	( 'VM_global_library.vm', NOW(), '#macro (testMacro $param) I am a macro using $param #end');
