import os
import pdb
from fabric.api import *
import time
import argparse
import ConfigParser

# Follows the format {'<Section in .ini file>' : '['<Key in .ini file (*)>', ...]', ..}
# Note: (*) The key here SHOULD match the key in the properties file which will read by the java code
MBS_PROP_KEYS = {
        'database' : ['db_name', 'db_port', 'db_username', 'db_password', 'db_connection_limit' , 'dbFillLevelPerQueue'], # Excludes db_url. This is auto added by reusing hostname.
        'middleware' : ['mw_message_handlers_pool_size', 'mw_port'],
        }

CLIENT_PROP_KEYS = {
         'middleware' : ['mw_message_handlers_pool_size', 'mw_port'],
         'database' : [ 'db_connection_limit', 'dbFillLevelPerQueue'], 
         'client_start_idx' : [ 'readone_peek', 'sender', 'request_response'],
         'experiment' : ['id','description','experiment_duration','num_middlewares','num_client_machines', 'debug_on']
        }


def install_basic_utils(user_name, hostname, path_to_key=None):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    sudo('yum install readline* -y')
    sudo('yum install tmux -y')
    sudo('yum install zlib* -y')
    sudo('yum install gcc -y')
    sudo('yum install dstat -y')


def setup_java_env(user_name, hostname, path_to_key=None):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    sudo('yum install java-1.7.0-openjdk -y')
    sudo('sudo alternatives --set java /usr/lib/jvm/jre-1.7.0-openjdk.x86_64/bin/java')
    sudo('chown -R ec2-user:ec2-user /local')


def setup_postgres(user_name, hostname, sql_script_path, pg_hba_conf_path, postgres_conf_path, postgres_source_path, is_ec2, path_to_key, db_name):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    if is_ec2:
        sudo('chown -R ec2-user:ec2-user /local')

    with warn_only():
        run('mkdir /local/user04')
        run('mkdir /local/user04/pgsql')

    put(postgres_source_path, '/local/user04/')
    with cd('/local/user04'):
        run('tar xf postgresql-9.3.5.tar.gz')
       #run('mv postgresql-9.2.4 pgsql')
        run('mv postgresql-9.3.5/* /local/user04/pgsql')
        
    with cd('/local/user04/pgsql'):
        run('./configure --prefix=/local/user04/pgsql')
        run('make')
        run('make install')

    with cd('/local/user04/pgsql'):
        with warn_only():
            run('mkdir data')
        with warn_only():
            run('chmod 777 data')
        run('bin/initdb /local/user04/pgsql/data')
        put(pg_hba_conf_path, '/local/user04/pgsql/data/pg_hba.conf')
        put(postgres_conf_path, '/local/user04/pgsql/data/postgresql.conf')
        put(sql_script_path, '/local/user04/create_schema.sql')
        run('bin/pg_ctl -D /local/user04/pgsql/data -l logfile start &', pty=False)
        time.sleep(5)
        run('bin/createdb %s' % db_name)
        run('bin/psql -f ../create_schema.sql %s' % db_name)

def restart_postgres(user_name, hostname,path_to_key):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname
    with cd('/local/user04/pgsql'):
       run('bin/pg_ctl -D /local/user04/pgsql/data -l logfile restart -m fast &', pty=False)

def postgres_service_cmd(user_name, hostname, path_to_key, operation):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    if operation not in ["start", "stop", "restart"]:
        sys.exit("Supported operations for postgres service: start, stop, restart")

    with cd('/local/user04/pgsql'):
        run('bin/pg_ctl -D /local/user04/pgsql/data -l logfile %s &' % operation, pty=False)


def setup_middleware_env(user_name, hostname, mw_jar_path, mw_prop_file_path, path_to_key, is_ec2):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    run('pwd')

    if is_ec2:
        sudo('chown -R ec2-user:ec2-user /local')
        install_basic_utils(user_name, hostname, path_to_key)
        setup_java_env(user_name, hostname, path_to_key)

    with warn_only():
        run('mkdir /local/user04')

    # Copy jar to remote machine
    put(mw_jar_path, '/local/user04')
    # Copy properties file to remote machine
    with warn_only():
        run('mkdir /local/user04/properties')
    put(mw_prop_file_path, '/local/user04/properties')


def setup_client_env(user_name, hostname, client_jar_path, client_prop_file_path, path_to_key, is_ec2):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    if is_ec2:
        sudo('chown -R ec2-user:ec2-user /local')
        install_basic_utils(user_name, hostname, path_to_key)
        setup_java_env(user_name, hostname, path_to_key)

    with warn_only():
        run('mkdir /local/user04')

    # Copy jar to remote machine
    put(client_jar_path, '/local/user04')
    # Copy properties file to remote machine
    with warn_only():
        run('mkdir /local/user04/properties')
    put(client_prop_file_path, '/local/user04/properties')


def reconstructdb(user_name, hostname, sql_script_path, path_to_key, db_name):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    with cd('/local/user04/pgsql'):
        run('bin/dropdb %s' % db_name)
        run('bin/createdb %s' % db_name)
        run('bin/psql -f ../create_schema.sql %s' % db_name)


def kill_java_processes(user_name, hostname, path_to_key):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    with warn_only():
        run("pkill -f 'java -jar'")
        run("killall java")


def copy_file_to_host(user_name, hostname, path_to_key, source_jar, dest):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    put(source_jar, dest)


def get_file_from_host(user_name, hostname, path_to_key, remote_path, local_path):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    get(remote_path, local_path)


def run_remote_cmd(user_name, hostname, path_to_key, command, use_cwd='/', pty=True):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    with cd(use_cwd):
        res = run(command, pty=pty)
    return res


def run_remote_sudo_cmd(user_name, hostname, path_to_key, command):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    res = sudo(command)
    return res


def execute_jar_on_host(user_name, hostname, path_to_key, jar_path, java_path, background=False, use_cwd='/', cmd_args=''):
    env.user = user_name
    if path_to_key:
        env.key_filename = path_to_key
    env.host_string = hostname

    with cd(use_cwd):
        if background:
            run("%s -jar %s %s &" % (java_path, jar_path, cmd_args), shell=False)
        else:
            run("%s -jar %s %s" % (java_path, jar_path, cmd_args))



def main():
    parser = argparse.ArgumentParser()
    # parser.add_argument("configfile", help="Full path to experiment's .ini file")
    parser.add_argument("experimentlist", help="List of experiment .ini files")
    parser.add_argument("--skippgsetup", help="Skip installation of postgres", action="store_true")
    parser.add_argument("--skiputilssetup", help="Skip installation of java and other utils", action="store_true")
    parser.add_argument("--skipCSV", help="Skip the extraction and CSV creation local steps", action="store_true")
    parser.add_argument("--skipDrop", help="Skip the extraction and CSV creation local steps", action="store_true")
      
    args = parser.parse_args()

    # Read list of experiment file paths from the file and store it in experiment_path_list
    experiment_path_list = []
    with open(args.experimentlist, 'r') as f:
        for line in f:
            experiment_path_list.append(line[:-1].strip())
    print experiment_path_list
    masterconfig = ConfigParser.ConfigParser()
    masterconfig.read(experiment_path_list[0])
	
    if not args.skippgsetup:
        ### Infrastructure Setup ###
        # 1. Set up Postgres machine
        # 1.a. Install necessary applications
        if masterconfig.get("setup", "is_ec2") in ["True", "true"]: # If host is on EC2 install gcc, etc.
            install_basic_utils(masterconfig.get("setup", "username"), # ec2-user or user04
                    masterconfig.get("hostnames", "db_server"),
                    masterconfig.get("paths", "pem_key_path"),
                    )

        setup_postgres(masterconfig.get("setup", "username"), # ec2-user or user04
                        masterconfig.get("hostnames", "db_server"),
                        masterconfig.get("paths", "db_schema_path"),
                        masterconfig.get("paths", "pg_hba_conf_path"),
                        masterconfig.get("paths", "postgres_conf_path"),
                        masterconfig.get("paths", "postgres_source_path"),
                        masterconfig.get("setup", "is_ec2") in ["True", "true"],
                        masterconfig.get("paths", "pem_key_path") if masterconfig.get("paths", "pem_key_path") != "None" else None,
                        masterconfig.get("database", "db_name")
                    )


    
    isMultiExp = False
    try: 
        isMultiExp = masterconfig.get("experiment", "multiHosts")
    except:
        pass                
    
    
    mw_dict = dict()
    cl_dict = dict()
    link_dict = dict()
    if isMultiExp:
        for propkey in masterconfig.options('middleHost'):
            mw_dict[propkey]=masterconfig.get('middleHost',propkey).strip()
        for propkey in masterconfig.options('clientHost'):
            cl_dict[propkey]=masterconfig.get('clientHost',propkey).strip()      
        for propkey in masterconfig.options('hostLinks'):
            link_dict[propkey] = masterconfig.get('hostLinks',propkey).strip(",")
                    
    if not args.skiputilssetup and not isMultiExp:
        # 2. Set up Middleware machine
        # 2.a. Install necessary applications
        if masterconfig.get("setup", "is_ec2") in ["True", "true"]: # If host is on EC2 install gcc, etc.
            install_basic_utils(masterconfig.get("setup", "username"), # ec2-user or user04
                    masterconfig.get("hostnames", "middleware"),
                    masterconfig.get("paths", "pem_key_path"),
                    )
            setup_java_env(masterconfig.get("setup", "username"), # ec2-user or user04
                    masterconfig.get("hostnames", "middleware"),
                    masterconfig.get("paths", "pem_key_path"),
                    )


        # 3. Set up Client machine
        # 3.a. Install necessary applications
        if masterconfig.get("setup", "is_ec2") in ["True", "true"]: # If host is on EC2 install gcc, etc.
            install_basic_utils(masterconfig.get("setup", "username"), # ec2-user or user04
                    masterconfig.get("hostnames", "client"),
                    masterconfig.get("paths", "pem_key_path"),
                    )
            setup_java_env(masterconfig.get("setup", "username"), # ec2-user or user04
                    masterconfig.get("hostnames", "client"),
                    masterconfig.get("paths", "pem_key_path"),
                    )
                    
    if not args.skiputilssetup and isMultiExp:      
        # 2. Set up Middleware machine
        # 2.a. Install necessary applications
        if masterconfig.get("setup", "is_ec2") in ["True", "true"]: # If host is on EC2 install gcc, etc.
            for mwHost in mw_dict.values():
                install_basic_utils(masterconfig.get("setup", "username"), # ec2-user or user04
                        mwHost,
                        masterconfig.get("paths", "pem_key_path"),
                        )
                setup_java_env(masterconfig.get("setup", "username"), # ec2-user or user04
                        mwHost,
                        masterconfig.get("paths", "pem_key_path"),
                        )

        # 3. Set up Client machine
        # 3.a. Install necessary applications
        if masterconfig.get("setup", "is_ec2") in ["True", "true"]: # If host is on EC2 install gcc, etc.
            for clHost in cl_dict.values():
                install_basic_utils(masterconfig.get("setup", "username"), # ec2-user or user04
                        clHost,
                        masterconfig.get("paths", "pem_key_path"),
                        )
                setup_java_env(masterconfig.get("setup", "username"), # ec2-user or user04
                        clHost,
                        masterconfig.get("paths", "pem_key_path"),
                        )

    
    
    ### Run experiments ###
    for experiment_config_path in experiment_path_list:
        
        
        exp_config = ConfigParser.ConfigParser()
        exp_config.read(experiment_config_path)

        exp_id = exp_config.get("experiment", "id")

        #test for Multi Middleware to Client connections 
        isMultiExp = False
        try: 
            isMultiExp = exp_config.get("experiment", "multiHosts")
        except:
            pass
            
        
        
        # Store commonly used config values into variables
        is_ec2 = exp_config.get("setup", "is_ec2") in ["True", "true"]
        java_path = exp_config.get("constants", "ec2_java7_path" if is_ec2 else "dryad_java7_path")
        pem_key_path = exp_config.get("paths", "pem_key_path") if exp_config.get("paths", "pem_key_path") != "None" else None
        setup_username = exp_config.get("setup", "username") # ec2-user or user04
        middleware_jar_name = exp_config.get("paths", "middleware_jar").split('/')[-1]
        client_jar_name = exp_config.get("paths", "client_jar").split('/')[-1]

        # Debugging stuff
        print "Experiment ID: ", exp_id
        print "Setup username: ", setup_username
        print "Private key file: ", pem_key_path
        print "Is EC2: ", is_ec2

        # Hosts
        
        db_host = exp_config.get("hostnames", "db_server")
        restart_postgres(masterconfig.get("setup", "username"), 
                    db_host,
                    masterconfig.get("paths", "pem_key_path"))
        if not isMultiExp:
            mw_host = exp_config.get("hostnames", "middleware")
            db_host = exp_config.get("hostnames", "db_server")
            cl_host = exp_config.get("hostnames", "client")
            

            ## I. Setup ##
            # Make /local accessible on middleware and client
            if is_ec2:
                run_remote_sudo_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        'chown -R ec2-user:ec2-user /local')
                run_remote_sudo_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        'chown -R ec2-user:ec2-user /local')

            # Create a local directory with experiment name
            operating_dir = exp_config.get("experiment", "id")
            # Initialize middleware with appropriate folders
            run_remote_cmd(setup_username,
                    mw_host,
                    pem_key_path,
                    "mkdir -p /local/user04/%s/properties" % exp_id)
            with cd(os.getcwd()): local("mkdir -p %s/middleware" % exp_id)
            with warn_only():
                run_remote_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        "mkdir -p /local/user04/logs",
                        )
            # Copy .ini file to remote instance
            copy_file_to_host(setup_username,
                            mw_host,
                            pem_key_path,
                            experiment_config_path,
                            "/local/user04/%s" % exp_id,
                            )

            # Initialize client with appropriate folders
            run_remote_cmd(setup_username,
                    cl_host,
                    pem_key_path,
                    "mkdir -p /local/user04/%s/properties" % exp_id)
            with cd(os.getcwd()): local("mkdir -p %s/client" % exp_id)
            with warn_only():
                run_remote_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        "mkdir -p /local/user04/logs",
                        )
            # Copy .ini file to remote instance
            copy_file_to_host(setup_username,
                            cl_host,
                            pem_key_path,
                            experiment_config_path,
                            "/local/user04/%s" % exp_id,
                            )

            # Start postgres server
            postgres_service_cmd(setup_username,
                    db_host,
                    pem_key_path,
                    "start")

            # Recreate database
            # This: a. Drops the database, b. Creates the database, c. Runs the sql script
            if not args.skipDrop:
                    reconstructdb(setup_username,
                            db_host,
                            exp_config.get("paths", "db_schema_path"),
                            pem_key_path,
                            exp_config.get("database", "db_name"))


             #2014   add for loop here to perform this for multiple middleware machiens         
            # Move MessageBrokerService jars to middleware
            
            copy_file_to_host(setup_username,
                            mw_host,
                            pem_key_path,
                            exp_config.get("paths", "middleware_jar"),
                            "/local/user04/%s" % exp_id,
                            )

            # Generate properties file for MessageBrokerService
            local_mbs_prop_path = '%s/mbs.properties' % operating_dir
            with open(local_mbs_prop_path, 'w') as mbs_pf:
                mbs_pf.write('db_url=%s\n' % exp_config.get("hostnames", "db_server"))
                for key in MBS_PROP_KEYS:
                    for propkey in MBS_PROP_KEYS[key]:
                        mbs_pf.write('%s=%s\n' % (propkey, exp_config.get(key, propkey)))
                for propkey in exp_config.options('middleware_props'):
                        mbs_pf.write('%s=%s\n' % (propkey, exp_config.get('middleware_props', propkey)))
            copy_file_to_host(exp_config.get("setup", "username"), # ec2-user or user04
                            exp_config.get("hostnames", "middleware"),
                            pem_key_path,
                            local_mbs_prop_path,
                            "/local/user04/%s/properties" % exp_id,
                            )

            copy_file_to_host(setup_username,
                    cl_host,
                    pem_key_path,
                    exp_config.get("paths", "client_jar"),
                    "/local/user04/%s" % exp_id,
                    )
                    


            # Generate properties file for Client
            local_client_prop_path = '%s/client.properties' % operating_dir
            with open(local_client_prop_path, 'w') as client_pf:
                client_pf.write('mw_url=%s\n' % exp_config.get("hostnames", "middleware"))    #2014 change this for multi client machine 
                # 1. Write predefined property keys
                for key in CLIENT_PROP_KEYS:
                    for propkey in CLIENT_PROP_KEYS[key]:
                        client_pf.write('%s=%s\n' % (propkey, exp_config.get(key, propkey)))
                # 4. Write all key-value pairs as is from the client_props section
                for propkey in exp_config.options('client_props'):
                        client_pf.write('%s=%s\n' % (propkey, exp_config.get('client_props', propkey)))
            copy_file_to_host(exp_config.get("setup", "username"), # ec2-user or user04
                            cl_host,
                            pem_key_path,
                            local_client_prop_path,
                            "/local/user04/%s/properties" % exp_id,
                            )

            # Delete existing screens by name "mbs" if any
            # with warn_only():
            #     run_remote_cmd(setup_username,
            #             mw_host,
            #             pem_key_path,
            #             "screen -X -S mbs quit",
            #             )


            ## II. Experiment ##
            # a. Start Message Broker Service
            mbs_jar_path = "/local/user04/%s/%s" % (exp_id, middleware_jar_name)
            # Stitch command-line arguments
            cmd_args=''
            for option in exp_config.options('middleware_cl_args'):
                cmd_args += '--%s %s ' % (option, exp_config.get('middleware_cl_args', option))
            # Execute jar as background process
            mbs_command = "%s -jar %s %s" % (java_path, mbs_jar_path, cmd_args)
            # Create new screen on middleware host to start service


            # Kill java processes (if any) on the middleware and client
            kill_java_processes(exp_config.get("setup", "username"), # ec2-user or user04
                            exp_config.get("hostnames", "middleware"),
                            pem_key_path,
                            )
            kill_java_processes(exp_config.get("setup", "username"), # ec2-user or user04
                            exp_config.get("hostnames", "client"),
                            pem_key_path,
                            )

            
            run_remote_cmd(setup_username,
                mw_host,
                pem_key_path,
                "dstat -Ttadmrsl > /tmp/dstat-%s.log &" % (exp_id),
                use_cwd='/local/user04/%s' % exp_id
                )
            run_remote_cmd(setup_username,
                mw_host,
                pem_key_path,
                "tmux new-session -d -n dstatT",
                use_cwd='/local/user04/%s' % exp_id
                )
            run_remote_cmd(setup_username,
                mw_host,
                pem_key_path,
                "tmux send-keys -t dstatT 'dstat -Ttadmrsl > /tmp/dstat-%s.log \n'" % (exp_id),
                use_cwd='/local/user04/%s' % exp_id
                ) 
               
            run_remote_cmd(setup_username,
                mw_host,
                pem_key_path,
                "tmux new-session -d -n mbsT",
                use_cwd='/local/user04/%s' % exp_id
                )
            run_remote_cmd(setup_username,
                mw_host,
                pem_key_path,
                "tmux send-keys -t mbsT '%s\n'" %mbs_command,
                use_cwd='/local/user04/%s' % exp_id
                )            

            # Execute setup_jar 
            if exp_config.get("paths", "setup_jar") != "None":
                cmd_args=''
                setup_jar_name = exp_config.get("paths", "setup_jar").split('/')[-1]
                for option in exp_config.options('setup_cl_args'):
                    cmd_args += '--%s %s ' % (option, exp_config.get('setup_cl_args', option))
                copy_file_to_host(setup_username,
                        cl_host,
                        pem_key_path,
                        exp_config.get("paths", "setup_jar"),
                        "/local/user04/%s" % exp_id,
                    )
                setup_jar_path_on_host = "/local/user04/%s/%s" % (exp_id, setup_jar_name)
                execute_jar_on_host(setup_username,
                                cl_host,
                                pem_key_path,
                                setup_jar_path_on_host,
                                java_path,
                                background=False,
                                use_cwd='/local/user04/%s' % exp_id,
                                cmd_args=cmd_args,
                                )

            # b. Spawn clients
            # client_jar_path = "/local/user04/%s/%s" % (exp_id, client_jar_name)
            # Stitch command-line arguments
            cmd_args=''
            for option in exp_config.options('client_cl_args'):
                cmd_args += '--%s %s ' % (option, exp_config.get('client_cl_args', option))
            # num_clients = exp_config.get('client_config', 'num_clients')
            cli_jar_path = "/local/user04/%s/%s" % (exp_id, client_jar_name)
            cli_command = "%s -jar %s %s" % (java_path, cli_jar_path, cmd_args)


            run_remote_cmd(setup_username,
                cl_host,
                pem_key_path,
                "tmux new-session -d -n cliT",
                use_cwd='/local/user04/%s' % exp_id
                )
            run_remote_cmd(setup_username,
                cl_host,
                pem_key_path,
                "tmux send-keys -t cliT '%s\n'" %cli_command,
                use_cwd='/local/user04/%s' % exp_id
                ) 
            
            #run_remote_cmd(setup_username,
            #    cl_host,
            #    pem_key_path,
            #    "%s > /tmp/stdOutClie%s.log &" % (cli_command, exp_id),
            #    use_cwd='/local/user04/%s' % exp_id
            #    )
            
           
            # C. Wait for clients to complete execution
            print "## Waiting for the experiment to finish"
            time.sleep(int(exp_config.get("experiment", "experiment_duration"))/1000 +5) 
            ## III. Tear-down ##
            # a. Shutdown service
            # b. Move log files on to local machine
            with warn_only():
                run_remote_cmd(setup_username,
                    mw_host,
                    pem_key_path,
                    "killall java",
                    )
                # Kill dstat
                run_remote_cmd(setup_username,
                    mw_host,
                    pem_key_path,
                    "killall python",
                    )
                # run_remote_cmd(setup_username,
                #         db_host,
                #         pem_key_path,
                #         "/local/user04/pgsql/bin/psql -o %s-spool.log -c 'select * from messages; select * from queues;' ASL_1" % exp_id,
                #         )
                # run_remote_cmd(setup_username,
                #         db_host,
                #         pem_key_path,
                #         "/local/user04/pgsql/bin/psql -o %s-msgcount-a.log -c 'select clientid, count(*) from messages group by clientid order by clientid;' ASL_1" % exp_id,
                #         )
                # run_remote_cmd(setup_username,
                #         db_host,
                #         pem_key_path,
                #         "/local/user04/pgsql/bin/psql -o %s-msgcount-b.log -c 'select clientid, queueid, count(*) from messages group by clientid, queueid order by clientid, queueid;' ASL_1" % exp_id,
                #         )
                run_remote_cmd(setup_username,
                        db_host,
                        pem_key_path,
                        "/local/user04/pgsql/bin/pg_dump ASL_1 -f /tmp/%s.sql" % (exp_id),
                        )
                get_file_from_host(setup_username,
                        db_host,
                        pem_key_path,
                        "/tmp/%s.sql" % (exp_id),
                        "%s/" % exp_id,
                        )
            with warn_only():
                run_remote_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        "mkdir middleware",
                        use_cwd='/local/user04/%s' % exp_id,
                        )
                run_remote_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        "mv /tmp/mesg*.log* /local/user04/%s/middleware/" % exp_id,
                        )
                run_remote_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        "cp /tmp/dstat-%s.log /local/user04/%s/middleware/" % (exp_id, exp_id),
                        )
                run_remote_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        "tar cvzf /local/user04/logs/%s-middleware-%s-logs.tgz middleware" % (exp_id,mw_host[:18]),
                        use_cwd='/local/user04/%s' % exp_id,
                        )
                get_file_from_host(setup_username,
                        mw_host,
                        pem_key_path,
                        "/local/user04/logs/%s-middleware-%s-logs.tgz" % (exp_id,mw_host[:18]),
                        "%s" % exp_id,
                        )
                run_remote_cmd(setup_username,
                    mw_host,
                    pem_key_path,
                    "rm /local/user04/%s/middleware/*.log  " % exp_id,
                    )                   
            with warn_only():
                run_remote_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        "mkdir client",
                        use_cwd='/local/user04/%s' % exp_id,
                        )
                run_remote_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        "mv /tmp/mesg*.log* /local/user04/%s/client/" % exp_id,
                        )
                run_remote_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        "tar cvzf /local/user04/logs/%s-client-%s-logs.tgz client" % (exp_id,cl_host[:18]),
                        use_cwd='/local/user04/%s' % exp_id,
                        )
                get_file_from_host(setup_username,
                        cl_host,
                        pem_key_path,
                        "/local/user04/logs/%s-client-%s-logs.tgz" % (exp_id,cl_host[:18]),
                        "%s" % exp_id,
                        )
                os.chdir("%s" % exp_id)
                os.system("mkdir %s" % cl_host)
                os.system("tar xvzf %s-client*.tgz -C %s" % (exp_id,cl_host))
                run_remote_cmd(setup_username,
                    cl_host,
                    pem_key_path,
                    "rm /local/user04/%s/client/*.log  " % exp_id,
                    )   
                run_remote_cmd(setup_username,
                    cl_host,
                    pem_key_path,
                    "rm /local/user04/%s/client/*.log.lck  " % exp_id,
                    ) 
                os.chdir(cl_host)
                os.chdir("client")
                os.system('grep -oP "CSVFRMT\]\K.*" mesg*.log > csvClients_%s.csv' % exp_id)
                os.system("sed -i '1i  experiment_description,time_start_experiment,exp_duration,num_mh_threads,num_middlewares,num_db_handlers,num_client_machines,num_queues,num_clients_perMachien,request_id,client_id,request_type,clThinkTime,clRoundTime,mwThinkTime,mwRoundTime,dbRoundTime,dbThinkTime,result,mwTimeInDBQ,clTimeinQ,Label,custom,clConnInitTime,clCloseTime,mwOpenStream,mwSendReadyMsg,mwTimeReceiveDate,mwEnterQ,mwLeaveQ,clEnterQ,clLeaveQ' csvClients_%s.csv" % exp_id )
                os.chdir("../../../")
            
            # Multi middle-ware to client setup
        else:
        
            mw_dict = dict()
            cl_dict = dict()
            link_dict = dict()
            if isMultiExp:
                for propkey in exp_config.options('middleHost'):
                    mw_dict[propkey]=exp_config.get('middleHost',propkey).strip()
                for propkey in exp_config.options('clientHost'):
                    cl_dict[propkey]=exp_config.get('clientHost',propkey).strip()      
                for propkey in exp_config.options('hostLinks'):
                    link_dict[propkey] = exp_config.get('hostLinks',propkey).strip(",")        

            operating_dir = exp_config.get("experiment", "id")
            ## II. Experiment ##
            # a. Start Message Broker Service
            mbs_jar_path = "/local/user04/%s/%s" % (exp_id, middleware_jar_name)
            # Stitch command-line arguments
            cmd_args=''
            for option in exp_config.options('middleware_cl_args'):
                cmd_args += '--%s %s ' % (option, exp_config.get('middleware_cl_args', option))
            # Execute jar as background process
            #mbs_command = "%s -jar %s %s" % (java_path, mbs_jar_path, cmd_args)
            mbs_command = "%s -jar %s %s &> %s" % (java_path, mbs_jar_path, cmd_args, "/tmp/midCoutCerr_%s_%s.log" % (exp_id, time.time() * 1000) )
            # Create new screen on middleware host to start service
            

            ############ DB Stuff ###########
            
            # Start postgres server
            postgres_service_cmd(setup_username,
                    db_host,
                    pem_key_path,
                    "start")

            # Recreate database
            # This: a. Drops the database, b. Creates the database, c. Runs the sql script
            if not args.skipDrop:            
                    reconstructdb(setup_username,
                            db_host,
                            exp_config.get("paths", "db_schema_path"),
                            pem_key_path,
                            exp_config.get("database", "db_name"))
            
            
            
            
            ############### MW Stuff #################
            for mw_key in mw_dict.keys():
                mw_host = mw_dict[mw_key]
                run_remote_sudo_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        'chown -R ec2-user:ec2-user /local')
                run_remote_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        "mkdir -p /local/user04/%s/properties" % exp_id)
                with cd(os.getcwd()): local("mkdir -p %s/middleware" % exp_id)
                with warn_only():
                    run_remote_cmd(setup_username,
                            mw_host,
                            pem_key_path,
                            "mkdir -p /local/user04/logs",
                            )
                # Copy .ini file to remote instance
                copy_file_to_host(setup_username,
                                mw_host,
                                pem_key_path,
                                experiment_config_path,
                                "/local/user04/%s" % exp_id,
                                ) 
                # Move MessageBrokerService jars to middleware
                copy_file_to_host(setup_username,
                                mw_host,
                                pem_key_path,
                                exp_config.get("paths", "middleware_jar"),
                                "/local/user04/%s" % exp_id,
                                )              
                # Generate properties file for MessageBrokerService
                local_mbs_prop_path = '%s/mbs.properties' % operating_dir
                with open(local_mbs_prop_path, 'w') as mbs_pf:
                    mbs_pf.write('db_url=%s\n' % exp_config.get("hostnames", "db_server"))
                    for key in MBS_PROP_KEYS:
                        for propkey in MBS_PROP_KEYS[key]:
                            mbs_pf.write('%s=%s\n' % (propkey, exp_config.get(key, propkey)))
                    for propkey in exp_config.options('middleware_props'):
                            mbs_pf.write('%s=%s\n' % (propkey, exp_config.get('middleware_props', propkey)))
                copy_file_to_host(exp_config.get("setup", "username"), # ec2-user or user04
                                mw_host,
                                pem_key_path,
                                local_mbs_prop_path,
                                "/local/user04/%s/properties" % exp_id,
                                )    
                # Kill java processes (if any) on the middleware and client
                kill_java_processes(exp_config.get("setup", "username"), # ec2-user or user04
                                mw_host,
                                pem_key_path,
                                )
                run_remote_cmd(setup_username,
                    mw_host,
                    pem_key_path,
                    "dstat -Ttadmrsl > /tmp/dstat-%s.log &" % (exp_id),
                    use_cwd='/local/user04/%s' % exp_id
                    )
                run_remote_cmd(setup_username,
                    mw_host,
                    pem_key_path,
                    "tmux new-session -d -n dstatT",
                    use_cwd='/local/user04/%s' % exp_id
                    )
                run_remote_cmd(setup_username,
                    mw_host,
                    pem_key_path,
                    "tmux send-keys -t dstatT 'dstat -Ttadmrsl > /tmp/dstat-%s.log \n'" % (exp_id),
                    use_cwd='/local/user04/%s' % exp_id
                    ) 
                   
                run_remote_cmd(setup_username,
                    mw_host,
                    pem_key_path,
                    "tmux new-session -d -n mbsT",
                    use_cwd='/local/user04/%s' % exp_id
                    )
                run_remote_cmd(setup_username,
                    mw_host,
                    pem_key_path,
                    "tmux send-keys -t mbsT '%s\n'" %mbs_command,
                    use_cwd='/local/user04/%s' % exp_id
                    )                                     
            ###########End MW Stuff####################     
                     
                     
            ##########Starting Client Stuff ###########
            for cl_key in cl_dict.keys():
                cl_host = cl_dict[cl_key]
                run_remote_sudo_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        'chown -R ec2-user:ec2-user /local')

                # Initialize client with appropriate folders
                run_remote_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        "mkdir -p /local/user04/%s/properties" % exp_id)
                with cd(os.getcwd()): local("mkdir -p %s/client" % exp_id)
                with warn_only():
                    run_remote_cmd(setup_username,
                            cl_host,
                            pem_key_path,
                            "mkdir -p /local/user04/logs",
                            )
                # Copy .ini file to remote instance
                copy_file_to_host(setup_username,
                                cl_host,
                                pem_key_path,
                                experiment_config_path,
                                "/local/user04/%s" % exp_id,
                                )
                copy_file_to_host(setup_username,
                        cl_host,
                        pem_key_path,
                        exp_config.get("paths", "client_jar"),
                        "/local/user04/%s" % exp_id,
                        )  
                        
                # Generate properties file for Client Multi 2014 version 
                local_client_prop_path = '%s/client.properties' % operating_dir
                with open(local_client_prop_path, 'w') as client_pf:
                    client_pf.write('mw_url=%s\n' % mw_dict[link_dict[cl_key]])    #2014 change this for multi client machine 
                    # 1. Write predefined property keys
                    for key in CLIENT_PROP_KEYS:
                        for propkey in CLIENT_PROP_KEYS[key]:
                            client_pf.write('%s=%s\n' % (propkey, exp_config.get(key, propkey)))
                    # 4. Write all key-value pairs as is from the client_props section
                    for propkey in exp_config.options('client_props'):
                            client_pf.write('%s=%s\n' % (propkey, exp_config.get('client_props', propkey)))
                copy_file_to_host(exp_config.get("setup", "username"), # ec2-user or user04
                                cl_host,
                                pem_key_path,
                                local_client_prop_path,
                                "/local/user04/%s/properties" % exp_id,
                                )                        
                                
                kill_java_processes(exp_config.get("setup", "username"), # ec2-user or user04
                                cl_host,
                                pem_key_path,
                                )   
                # Execute setup_jar 
                if exp_config.get("paths", "setup_jar") != "None":
                    cmd_args=''
                    setup_jar_name = exp_config.get("paths", "setup_jar").split('/')[-1]
                    for option in exp_config.options('setup_cl_args'):
                        cmd_args += '--%s %s ' % (option, exp_config.get('setup_cl_args', option))
                    copy_file_to_host(setup_username,
                            cl_host,
                            pem_key_path,
                            exp_config.get("paths", "setup_jar"),
                            "/local/user04/%s" % exp_id,
                        )
                    setup_jar_path_on_host = "/local/user04/%s/%s" % (exp_id, setup_jar_name)
                    execute_jar_on_host(setup_username,
                                    cl_host,
                                    pem_key_path,
                                    setup_jar_path_on_host,
                                    java_path,
                                    background=False,
                                    use_cwd='/local/user04/%s' % exp_id,
                                    cmd_args=cmd_args,
                                    )     
                # b. Spawn clients
                # client_jar_path = "/local/user04/%s/%s" % (exp_id, client_jar_name)
                # Stitch command-line arguments
                cmd_args=''
                for option in exp_config.options('client_cl_args'):
                    cmd_args += '--%s %s ' % (option, exp_config.get('client_cl_args', option))
                # num_clients = exp_config.get('client_config', 'num_clients')
                cli_jar_path = "/local/user04/%s/%s" % (exp_id, client_jar_name)
                #cli_command = "%s -jar %s %s" % (java_path, cli_jar_path, cmd_args)
                cli_command = "%s -jar %s %s &> %s" % (java_path, cli_jar_path, cmd_args, "/tmp/cliCoutCerr_%s_%s.log" % (exp_id, time.time() * 1000) )
 
                run_remote_cmd(setup_username,
                    cl_host,
                    pem_key_path,
                    "tmux new-session -d -n cliT",
                    use_cwd='/local/user04/%s' % exp_id
                    )
                run_remote_cmd(setup_username,
                    cl_host,
                    pem_key_path,
                    "tmux send-keys -t cliT '%s\n'" %cli_command,
                    use_cwd='/local/user04/%s' % exp_id
                    ) 
        
            
            # C. Wait for clients to complete execution
            print "## Waiting for the experiment to finish"
            print (time.strftime("%H:%M:%S"))
            time.sleep(int(exp_config.get("experiment", "experiment_duration"))/1000 +10) 
             
             
             
            for cl_key in cl_dict.keys():
                cl_host = cl_dict[cl_key]                   
                with warn_only():
                    run_remote_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        "killall java",
                        ) 
                    run_remote_cmd(setup_username,
                            cl_host,
                            pem_key_path,
                            "mkdir client",
                            use_cwd='/local/user04/%s' % exp_id,
                            )
                    run_remote_cmd(setup_username,
                            cl_host,
                            pem_key_path,
                            "mv /tmp/mesg*.log* /local/user04/%s/client/" % exp_id,
                            )
                    run_remote_cmd(setup_username,
                            cl_host,
                            pem_key_path,
                            "mv /tmp/cliCoutCerr*.log /local/user04/%s/client/" % exp_id,
                            )
                    run_remote_cmd(setup_username,
                            cl_host,
                            pem_key_path,
                            "tar cvzf /local/user04/logs/%s-client-%s-logs.tgz client" % (exp_id,cl_host[:18]),
                            use_cwd='/local/user04/%s' % exp_id,
                            )
                    get_file_from_host(setup_username,
                            cl_host,
                            pem_key_path,
                            "/local/user04/logs/%s-client-%s-logs.tgz" % (exp_id,cl_host[:18]),
                            "%s" % exp_id,
                            )
                    run_remote_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        "rm /local/user04/%s/client/*.log  " % exp_id,
                        )   
                    run_remote_cmd(setup_username,
                        cl_host,
                        pem_key_path,
                        "rm /local/user04/%s/client/*.log.lck  " % exp_id,
                        )                                 
                
                
            for mw_key in mw_dict.keys():
                mw_host = mw_dict[mw_key]
                ## III. Tear-down ##
                # a. Shutdown service
                with warn_only():
                    run_remote_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        "killall java",
                        )
                    # Kill dstat
                    run_remote_cmd(setup_username,
                            mw_host,
                            pem_key_path,
                            "killall python",
                            )
                    run_remote_cmd(setup_username,
                            mw_host,
                            pem_key_path,
                            "mkdir middleware",
                            use_cwd='/local/user04/%s' % exp_id,
                            )
                    run_remote_cmd(setup_username,
                            mw_host,
                            pem_key_path,
                            "mv /tmp/mesg*.log* /local/user04/%s/middleware/" % exp_id,
                            )
                    run_remote_cmd(setup_username,
                            mw_host,
                            pem_key_path,
                            "cp /tmp/dstat-%s.log /local/user04/%s/middleware/" % (exp_id, exp_id),
                            )
                    run_remote_cmd(setup_username,
                            mw_host,
                            pem_key_path,
                            "mv /tmp/midCoutCerr*.log /local/user04/%s/middleware/" % exp_id,
                            )
                    run_remote_cmd(setup_username,
                            mw_host,
                            pem_key_path,
                            "tar cvzf /local/user04/logs/%s-middleware-%s-logs.tgz middleware" % (exp_id,mw_host[:18]),
                            use_cwd='/local/user04/%s' % exp_id,
                            )
                    get_file_from_host(setup_username,
                            mw_host,
                            pem_key_path,
                            "/local/user04/logs/%s-middleware-%s-logs.tgz" % (exp_id,mw_host[:18]),
                            "%s" % exp_id,
                            )
                    run_remote_cmd(setup_username,
                        mw_host,
                        pem_key_path,
                        "rm /local/user04/%s/middleware/*.log  " % exp_id,
                        )                     
                
            # b. Move DB log files on to local machine
            with warn_only():
                run_remote_cmd(setup_username,
                        db_host,
                        pem_key_path,
                        "/local/user04/pgsql/bin/pg_dump ASL_1 -f /tmp/%s.sql" % (exp_id),
                        )
                get_file_from_host(setup_username,
                        db_host,
                        pem_key_path,
                        "/tmp/%s.sql" % (exp_id),
                        "%s/" % exp_id,
                        )
            if not args.skipCSV:
                os.chdir("%s" % exp_id)
                os.system("ls *.tgz | xargs -i tar xfvz {}")                    
                os.chdir("client")
		os.system("echo 'experiment_description,time_start_experiment,exp_duration,num_mh_threads,num_middlewares,num_db_handlers,num_client_machines,num_queues, num_clients_perMachien ,request_id,client_id,request_type,clThinkTime,clRoundTime,mwThinkTime,mwRoundTime,dbRoundTime,dbThinkTime,result,mwTimeInDBQ,clTimeinQ,Label,custom,clConnInitTime,clCloseTime,mwOpenStream,mwSendReadyMsg,mwTimeReceiveDate,mwEnterQ,mwLeaveQ,clEnterQ,clLeaveQ,dbQsize,mwQsize,mwNoQRound,mwNetworkTime,mwID,myReadNetTime' > csvClients_%s.csv" % exp_id)
                os.system('grep -oP "CSVFRMT\]\K.*" mesg*.log >> csvClients_%s.csv' % exp_id)

                os.chdir("../../")
            
            

if __name__ == '__main__':
    main()
