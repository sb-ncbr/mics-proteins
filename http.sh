#!/bin/bash

#
#  This file is part of MESSIF library.
#
#  MESSIF library is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  MESSIF library is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
#

##################### DEFAULT CONFIG #####################
dir=`dirname $0`
logdir="$dir/logs"
piddir="$dir/pids"
javahome=
javaparams="-Xmx1024M"
mainclass='messif.utility.Application'
classpath=
classdir="$dir/jars"
debugport=
debugsuspend='n'
jmxport=
profiler=
batchfile=
waittime=0
remapfile='deserialize.map'
serfiles=()
sergc=0
rmi=
rmiwait=3
verbose=
starthosts=()
respawn=
autostart=
description=
librariesdir='/usr/local/lib'

##################### INTERNAL APP CONFIG #####################
java='java --add-opens java.base/java.lang.invoke=ALL-UNNAMED -cp \"$classpath\" $javaparams'
ssh='sshwrap'
telnet='/usr/bin/telnet'
ping='ping -c1 -q'
debug='-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=$debugsuspend,address='
jmx='-Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port='
jstat='jstat'

# Not really working nicely - linux uses ':'
# classpathsep=`java -h 2>&1 | sed -n 's/.*A \(.\) separated list of directories.*/\1/p'`
# if [ -z "$classpathsep" ];then
# classpathsep=':'
# fi

classpathsep=':'


##################### USAGE #####################

function usage() {
	echo "Usage: $0 [<options>] <action> [<host>:<port> ...]" >&2
	if [ "$1" != "full" ];then
		echo "	Use $0 --help for more informations" >&2
		exit 1
	fi
	echo -e "\n  ACTIONS" >&2
	echo "	start       ... start MESSIF Application on every specified host:port" >&2
	echo "	info        ... displays description, start hosts, and autostart" >&2
	echo "	stop        ... stop every process that has a PID in $piddir" >&2
	echo "	quit        ... kill (clean by SIGTERM) every process that has a PID in $piddir" >&2
	echo "	murder      ... kill (dirty by SIGKILL) every process that has a PID in $piddir" >&2
	echo "	pause       ... pause every process that has a PID in $piddir" >&2
	echo "	continue    ... continue every (paused) process that has a PID in $piddir" >&2
	echo "	check       ... check if processes that have a PID in $piddir are running" >&2
	echo "	mem         ... get RSS memory of every process that has a PID in $piddir" >&2
	echo "	fullmem     ... get Java memory utilization of every process" >&2
	echo "	command     ... execute Application's command on every process" >&2
	echo "	bgcommand   ... execute a command on every process in parallel" >&2
	echo -e "\n  DIRECTORY OPTIONS" >&2
	echo "	--dir <dir>			... $dir" >&2
	echo "	--logdir <dir>			... $logdir" >&2
	echo "	--piddir <dir>			... $piddir" >&2
	echo "	--javahome <path>		... ${javahome:-$JAVA_HOME}" >&2
	echo "	--classdir <dir with jars>	... ${classdir:-none}" >&2
	echo -e "\n  JAVA RUN OPTIONS" >&2
	echo "	--classpath <class path>	... ${classpath:-none}" >&2
	echo "	--debug[=<port>]		... ${debugport:-off}" >&2
	echo "	--debug-suspend			... $debugsuspend" >&2
	echo "	--jmx[=<port>]			... ${jmxport:-off}" >&2
	echo "	--profiler <path>		... ${profiler:-off}" >&2
	echo "	--javaparams <Java params>	... ${javaparams:-none}" >&2
	echo "	--mainclass <main class to run>	... $mainclass" >&2
	echo "	--ping [<ping command>]		... ${ping:-off}" >&2
	echo "	--respawn			... ${respawn:-off}" >&2
	echo -e "\n  BATCH OPTIONS" >&2
	echo "	--waittime <seconds>		... $waittime" >&2
	echo "	--batchfile <file>		... ${batchfile:-none}" >&2
	echo "	--batchcmd <name>		... ${batchcmd:-none}" >&2
	echo "	--batchfilevar <name>=<file>" >&2
	echo "	--batchvar <name>=value" >&2
	echo "		(expands '\$number', '\$port' or '\$host' variables)" >&2
	echo "	--batchvarformat <name>=format:value" >&2
	echo "		(expands '\$number', '\$port' or '\$host' variables)" >&2
	echo "	--rmi <port>[=<host>:<port>]	... ${rmi:-off}" >&2
	echo "	--rmiwait <time (seconds)>	... ${rmiwait:-1}" >&2
	echo -e "\n  DESERIALIZATION OPTIONS" >&2
	echo "	--serfiles <files>		... ${serfiles:-none}" >&2
	echo "	--sergc <time>			... ${sergc:-off}" >&2
	echo "	--remap orghost:orgport=newport	... ${remap:-off}" >&2
	echo "	--remap-file <filename>		... ${remapfile}" >&2
	echo -e "\n  SETTINGS DEBUG OPTIONS" >&2
	echo -n "	--dryrun			... " >&2
	if [ "$ssh" = "dryrun" ];then echo "on" >&2; else echo "off" >&2;fi
	echo "	--verbose			... ${verbose:-off}" >&2
	echo -e "\n  Default values are read from '${0%.sh}.defaults'" >&2
	echo "  Additionally, the defaults file can contain other variables:" >&2
	echo "    starthosts ... array of host:port pairs on which to start" >&2
	echo "    action ... default action used if none was on cmdline (and for autostart)" >&2
	echo "    description ... descriptive string shown by 'info' action" >&2

	if [ "$verbose" = "on" ];then
		echo -en "\n  Executed java command:\n\t"
		eval "echo $java $mainclass $batchfile $batchcmd \$vars"
		if [ ${#starthosts[@]} -gt 0 ];then
			echo -e "\n  Start on hosts: \n\t${starthosts[@]}"
		fi
	fi
	exit 1
}


##################### PARSE ARGUMENTS #####################

function batchvarformat() {
	vars="$vars ${1%%=*}"
	value="${1#*=}"
	vars="$vars='\`printf '${value%%:*}' \"${value#*:}\"\`'"
}

function batchvar() {
	vars="$vars ${1%%=*}='${1#*=}'"
}

function batchfilevar() {
	var_name=${1%%[=:]*}
	i=1
	while read val;do
		eval "filevar_$var_name[\$((i++))]=\$val"
	done < ${1#*[=:]}
	vars="$vars $var_name='\${filevar_$var_name[\$number]}'"
}

# Overridable function called before action (action name passed as 1st arg)
function onbeforeaction() {
	return;
}

# Overridable function called after action (action name passed as 1st arg)
function onafteraction() {
	return;
}

# Overridable function called before the start action with the host, port, and number arguments
function onbeforestart() {
	return;
}


### Override defaults if file exists
if [ -r "${0%.sh}.defaults" ];then
	source "${0%.sh}.defaults"
fi
showusage=0

args=`getopt -o '' -a -n "$0" -l help,dryrun,verbose,debug::,debug-suspend,jmx::,profiler:,classpath:,classdir:,logdir:,piddir:,dir:,batchfile:,batchcmd:,batchvar:,batchvarformat:,batchfilevar:,javahome:,javaparams:,mainclass:,respawn,ping::,waittime:,serfiles:,sergc:,remap:,remap-file:,rmi:,rmiwait: -- "$@"`
if [ $? != 0 ] ; then echo "Error: $args" >&2; usage; fi
eval set -- "$args"
while true ; do
	case "$1" in
		--help) showusage=1; shift;;
		--dryrun) ssh='dryrun'; telnet='dryrun'; shift;;
		--verbose) verbose='on'; shift;;
		--debug) debugport="${2:-port+1000}"; shift 2;;
		--debug-suspend) debugsuspend='y'; shift;;
		--jmx) jmxport="${2:-port+2000}"; shift 2;;
		--profiler) profiler="$2/lib";shift 2;;
		--classpath) classpath=$2; shift 2;;
		--classdir) classdir="$2"; shift 2;;
		--javahome) javahome="$2"; shift 2;;
		--javaparams) javaparams="$2"; shift 2;;
		--mainclass) mainclass="$2"; shift 2;;
		--respawn) respawn='on'; shift;;
		--ping) ping="$2"; shift 2;;
		--batchfile) batchfile="$2"; shift 2;;
		--batchcmd) batchcmd="$2"; shift 2;;
		--piddir) piddir="$2"; shift 2;;
		--logdir) logdir="$2"; shift 2;;
		--dir) dir="$2"; shift 2;;
		--waittime) waittime="$2"; shift 2;;
		--batchfilevar) batchfilevar "$2"; shift 2;;
		--batchvar) batchvar "$2"; shift 2;;
		--batchvarformat) batchvarformat "$2"; shift 2;;
		--serfiles) serfiles=($2); shift 2;;
		--sergc) sergc="$2"; shift 2;;
		--remap-file) remapfile="$2"; shift 2;;
		--remap) remap="$2"; shift 2;;
		--rmi) rmi="$2"; shift 2;;
		--rmiwait) rmiwait="$2"; shift 2;;
		--) shift; break;;
	esac
done

### Expand class dir
if [ -d "$classdir" ];then
	if [ -z "$classpath" ]; then
		classpath="$classdir"
	else
		classpath="$classpath$classpathsep$classdir"
	fi
	for file in "$classdir/"*.jar;do
		if [ -r "$file" ];then
			classpath="$classpath$classpathsep$file"
		fi
	done
fi

### Add standard path to java.library.path
java="$java -Djava.library.path=$librariesdir"

### Expand debug, jmx, profiler and respawn
if [ -n "$debugport" ];then java="$java $debug\$(($debugport))";fi
if [ "$debugsuspend" != 'y' ];then debugsuspend='n';fi
if [ -n "$jmxport" ];then java="$java $jmx\$(($jmxport))";fi
if [ -n "$profiler" ];then
	profiler_agnt=`find "$profiler" -type f -name 'libprofilerinterface.so' -path '*/jdk16/*' -printf '%p'`
	if [ -z "$profiler_agnt" ];then
		echo "Profiler directory is invalid" >&2
		usage
	fi
	java="$java -agentpath:$profiler_agnt=$profiler,5140"
fi
if [ "$respawn" != "on" ];then respawn="";fi

### Expand remap argument
if [ -n "$remap" ];then
	declare -i dstport="${remap#*=}"
	orghost="${remap%%=*}"
	declare -i orgport="${orghost##*:}"
	orghost="${orghost%:*}"
	if [ ! "$dstport" -gt 0 -o ! "$orgport" -gt 0 -o "$orghost" = "$orgport" ];then
		echo "Remap argument has wrong syntax" >&2
		usage
	fi
fi

### Make dir absolute
if [ "$dir" = "." ];then
	dir=`pwd`
elif [ "${dir:0:1}" != "/" ];then
	dir=`pwd`"/$dir"
fi

### Derive overlay name from directory
if [ -z "$name" ];then
	name="${dir##*/}"
fi

### Show usage on help
if [ "$showusage" = "1" ];then
	usage 'full'
fi

### Enable autostart if the action was set from defaults file
if [ -n "$action" ];then
	autostart=1
fi

### Read action
if [ $# -ge 1 ];then
	action=$1
	shift
fi
if [ "$action" = 'deserialize' ];then
	echo "Deprecated: deserialize action is set automatically if serfiles option is given";
fi
if [ "$action" = 'start' -a -n "$serfiles" ];then
	action='deserialize'
fi

### Read starthosts
if [ $# -ge 1 ];then
	starthosts=("$@")
elif [ ${#starthosts[@]} = 0 -a \( "$action" = 'start' -o "$action" = 'deserialize' \) ];then
	echo "No hosts to start (add starthosts array to defaults?)" >&2
	usage
fi

##################### HELPER METHODS #####################

function sshwrap() {
	short_host=`hostname -s 2>/dev/null`
	long_host=`hostname -f 2>/dev/null`
	if [ -z "$short_host" ];then short_host=`hostname 2>/dev/null`;fi
	if [ "$1" = 'localhost' -o "$1" = "$short_host" -o "$1" = "$long_host" ];then
		cat | /bin/bash -l
	else
		cat | ssh -T -p 22 "$@" /bin/bash -l
	fi
}

function dryrun() {
	echo
	if [ $# -ge 2 ];then echo "Executing on $1:$2";fi
	echo "-----------------------------------------"
	cat
	echo "-----------------------------------------"
}

# Helper function that expands the pid files
function expand_pids() {
	if [ $# = 0 ];then
		ls "$piddir"/*.pid 2>/dev/null
	else
		for file;do
			if [ -f "$piddir/$file.pid" ];then
				echo "$piddir/$file.pid"
			fi
		done
	fi
}

# Function for ssh execution on all pids (according to pid files)
function execute() {
	message="$1"
	cmd="$2"
	shift 2
	expand_pids "$@" | awk -F':|/|\\.pid' \
		'{hosts[$(NF-2)]=hosts[$(NF-2)] " " $0; hc[$(NF-2)]++}
		END{for (host in hosts) print host " " hc[host] hosts[host]}' |\
	while read host pidcount pidfiles;do
		echo -n "$message on $host"
		if [ "$verbose" = "on" ];then echo -n " (`cat $pidfiles`)";fi
		echo -n ": "
		(echo "cd '$dir'"; eval echo "\"$cmd\"") | $ssh "$host"
	done
}

# Function for telnet execution
function exec_batch() {
	message="$1"
	cmd="$2"
	shift 2
	declare -i number=0
	expand_pids "$@" | while read file;do
		if [ ! -r "$file" ];then continue;fi
		number=number+1
		file=${file#$piddir/}
		file=${file%.pid}
		echo "$message on $file"
		local host="${file%:*}"
		local port="${file#*:}"
		(eval echo \"$cmd\";while [ "$telnet" != 'dryrun' ];do echo 'close';sleep 1;done) | $telnet $host $port >&2 &
		if [ "$action" = "bgcommand" ];then sleep $waittime; else wait $!; fi
	done
	if [ "$action" = "bgcommand" ];then wait;fi
}

# Function to generate map file
function generate_map_file() {
	i=0
	while [ $i -lt ${#serfiles[@]} ];do
		for sth in "${starthosts[@]}";do
			echo "$orghost:$((orgport+i))=${sth%:*}:$((dstport+i))"
			i=$((i+1))
		done
	done
}

# Function that provides a deserialize script
function generate_start_script() {
	local host=$1
	local port=$2
	local number=$3

	# Setup java home
	if [ -n "$javahome" ];then
		echo "export JAVA_HOME='$javahome'"
		echo "export PATH=\"$javahome/bin:\$PATH\""
	fi

	# Setup directory
	echo "cd '$dir'"
	echo "mkdir -p '$logdir'"
	echo "mkdir -p '$piddir'"

	# Rebuild map file (process-safe)
	if [ -n "$remap" ];then
		echo "cat <<EOF > $remapfile.\$\$"
		generate_map_file
		echo "EOF"
		echo "mv -f $remapfile.\$\$ $remapfile"
	fi

	# Print start date
	echo "echo -n 'App started at ' >> '$logdir/$host:$port.log'"
	echo "date >> '$logdir/$host:$port.log'"

	# Respawn cycle
	if [ "$respawn" = "on" ];then
		echo 'function respawn() {'
		echo "trap 'kill \$javapid;exit 0' SIGTERM"
		echo 'while :;do'
	fi

	# Show command
	if [ "$action" = "deserialize" ];then
		cmd='- <<EOF'
	elif [ -n "$batchfile" ]; then
		cmd="'\$batchfile' $vars \$batchcmd"
	else
		cmd=''
	fi
	eval echo -n \"$java \$mainclass \$port $cmd\"
	echo " >> '$logdir/$host:$port.log' 2>&1 &"

	# Generate deserialize script
	if [ "$action" = "deserialize" ];then
		echo -n "actions = deserialize"
		if [ "$sergc" -gt 0 ];then
			echo -n " sergc"
		fi
		echo -en "\nactions.foreach ="
		local j=$((number-1))
		while [ $j -lt ${#serfiles[@]} ];do
			echo -n " ${serfiles[$j]}"
			j=$((j+${#starthosts[@]}))
		done
		echo
		echo "deserialize = algorithmRestore"
		echo "deserialize.param.1 = <actions>"
		if [ -f "$remapfile" -o -n "$remap" ];then
			echo "deserialize.param.2 = $remapfile"
		fi
		echo "sergc = collectGarbage"
		echo "sergc.param.1 = $sergc"
		echo "EOF"
	fi

	# End of respawn cycle
	if [ "$respawn" = "on" ];then
		echo 'javapid=$!'
		echo 'wait'
		echo "echo -n 'App restarted at ' >> '$logdir/$host:$port.log'"
		echo "date >> '$logdir/$host:$port.log'"
		echo 'done'
		echo '}';
		echo 'respawn >/dev/null 2>&1 &'
	fi

	# Store PID file (note that when respawn is on the pid is the script)
	echo "echo \$! > '$piddir/$host:$port.pid'"

	# Sleep waittime seconds
	echo "sleep $waittime"
}

function start() {
	local host=$1
	local port=$2
	local number=$3

	if [ -s "$piddir/$host:$port.pid" -a "$ssh" != "dryrun" ];then
		echo "Can't start $host:$port - pid file exists"
	else
		echo -n "Starting $host:$port "
		if [ -n "$debugport" ];then eval echo -n '"(debugger: $(('$debugport'))) "';fi
		echo -n "(count: $number): "
		if [ -z "$ping" ] || $ping $host > /dev/null 2>&1 ; then
			generate_start_script > "${0%.sh}-finalStartScript.sh"
			generate_start_script $host $port $number | $ssh $host
			echo "started"
		else
			echo "not alive"
		fi
	fi
}


##################### ACTION STARTUP #####################

onbeforeaction $action

case "$action" in
start|deserialize)
	number=0
	for i in "${starthosts[@]}"; do
		# Parse host and port
		host="${i%:*}"
		declare -i port="${i#*:}"
		if [ "$host" = "$i" -o "$port" -eq 0 ];then
			echo "Invalid host:port syntax: '$i'" >&2
			exit 1
		fi
		number=$((number+1))
		onbeforestart $host $port $number
		start $host $port $number
	done

	# Start RMI
	if [ -n "$rmi" ];then
		sleep ${rmiwait:-1}
		rmiport=${rmi%%=*}
		if [ "$rmiport" != "$rmi" ];then
			exec_batch 'Starting RMI' "rmiStart $rmiport" "${rmi#*=}"
		else
			exec_batch 'Starting RMI' "rmiStart $rmiport" "${starthosts[@]}"
		fi
	fi
	;;
stop)
	exec_batch 'Stopping algorithms' 'algorithmStopAll' "${starthosts[@]}" 2>/dev/null
	execute 'Stopping' 'kill \`cat $pidfiles\` && rm $pidfiles;echo done' ${starthosts[@]}
	;;
quit)
	execute 'Stopping' 'kill \`cat $pidfiles\` && rm $pidfiles;echo done' ${starthosts[@]}
	;;
murder)
	execute 'Killing (-9)' 'kill -9 \`cat $pidfiles\` && rm $pidfiles;echo done' ${starthosts[@]}
	;;
pause)
	execute 'Pausing' 'kill -SIGSTOP \`cat $pidfiles\`;echo done' ${starthosts[@]}
	;;
continue)
	execute 'Continuing' 'kill -SIGCONT \`cat $pidfiles\`;echo done' ${starthosts[@]}
	;;
	
stack)
	execute 'Writing stack' 'kill -3 \`cat $pidfiles\`;echo done' ${starthosts[@]}
	;;
mem)
	execute 'Total memory used' 'ps -o rss= \`cat $pidfiles\` | awk '"'{c++;i+=\\\$0}END{print i \\\" total, \\\" i/c \\\" average\\\"}'" ${starthosts[@]}
	;;
fullmem)
	execute 'Memory' 'echo;for pidfile in $pidfiles;do pid=\`cat \$pidfile\`;echo -n \"\`ps -o rss= \$pid\` \";$jstat -gc \$pid|tail -1;done' ${starthosts[@]}
	;;
check)
	execute 'Checking' 'ps -o user= -o comm= -o pid= -p \`cat $pidfiles\` | awk \"{cnt++}END{print cnt \\\" running, \\\" ($pidcount - cnt) \\\" dead\\\"}\"' ${starthosts[@]}
	;;
command|bgcommand)
	if [ -n "$batchfile" ]; then
		exec_batch "Calling $action" "controlFile \$batchfile $vars \$batchcmd" "${starthosts[@]}"
	elif [ -n "$batchcmd" ]; then
		exec_batch "Calling $action" '$batchcmd' "${starthosts[@]}"
	else
		echo "Neither --batchfile nor --batchcmd was specified" >&2
		usage
	fi
	;;
info)
	echo -n "Overlay '$name' "
	if [ "$autostart" = "1" ];then echo -n "auto";fi
	echo "starts on ${starthosts[*]}"
	if [ "$verbose" = "on" -a -n "$description" ];then
		echo "Description: $description"
	fi
	;;
*)
	echo "Unknown action: $action" >&2
	usage
	;;
esac

onafteraction $action
