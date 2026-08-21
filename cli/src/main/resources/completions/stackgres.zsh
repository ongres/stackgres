compdef _stackgres stackgres


function _stackgres() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-E,--environment}"[The target environment id]:environment:__stackgres_list_environments" \
    "--context[Use a named context from ~/.stackgres/config.yaml]:context:__stackgres_list_contexts" \
    "1: :(cluster environment context node slon login logout status whoami)" \
    "*::arg:->args"

  case $line[1] in
    cluster)
      _stackgres_cluster
    ;;
    environment|env)
      _stackgres_environment
    ;;
    context|ctx)
      _stackgres_context
    ;;
    node)
      _stackgres_node
    ;;
    slon)
      _stackgres_slon
    ;;
    login)
      _stackgres_login
    ;;
    logout)
      _stackgres_logout
    ;;
    status)
      _stackgres_status
    ;;
    whoami)
      _stackgres_whoami
    ;;
  esac

}

function _stackgres_cluster() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1: :(get list create delete start stop restart logs exec psql tunnel version extension metrics)" \
    "*::arg:->args"

  case $line[1] in
    get)
      _stackgres_cluster_get
    ;;
    list)
      _stackgres_cluster_list
    ;;
    create)
      _stackgres_cluster_create
    ;;
    delete)
      _stackgres_cluster_delete
    ;;
    start)
      _stackgres_cluster_start
    ;;
    stop)
      _stackgres_cluster_stop
    ;;
    restart)
      _stackgres_cluster_restart
    ;;
    logs)
      _stackgres_cluster_logs
    ;;
    exec)
      _stackgres_cluster_exec
    ;;
    psql)
      _stackgres_cluster_psql
    ;;
    tunnel)
      _stackgres_cluster_tunnel
    ;;
    version)
      _stackgres_cluster_version
    ;;
    extension)
      _stackgres_cluster_extension
    ;;
    metrics)
      _stackgres_cluster_metrics
    ;;
  esac

}

function _stackgres_cluster_get() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-E,--environment}"[The target environment id]:environment:__stackgres_list_environments" \
    "--context[Use a named context]:context:__stackgres_list_contexts" \
    "1:cluster:__stackgres_list_all_clusters" \
    "*::arg:->args"

}

function _stackgres_cluster_list() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-E,--environment}"[The target environment id]:environment:__stackgres_list_environments" \
    {-A,--all-environments}"[List across all environments (the default when no environment is active)]" \
    "--context[Use a named context]:context:__stackgres_list_contexts" \
    "--show-tags[Shows the cluster tags in the list]" \
    {-q,--quiet}"[Only display the cluster names]" \
    {-t,--tag}"[Only list clusters that are tagged accordingly]:arg:->args" \
    "*::arg:->args"

}

function _stackgres_cluster_create() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-E,--environment}"[The target environment id]:environment:__stackgres_list_environments" \
    "--context[Use a named context]:context:__stackgres_list_contexts" \
    {-n,--name}"[The cluster name]:arg:->args" \
    {-F,--flavor}"[The database flavor]:flavor:(postgres ivorysql)" \
    {-v,--version}"[The PostgreSQL version]:version:__stackgres_list_versions" \
    {-p,--port}"[The port of the PostgreSQL cluster. Specifying 0 will randomly choose a free port.]:arg:->args" \
    "--ivorysql-port[The IvorySQL Oracle-compatible listener port (only valid for flavor ivorysql)]:arg:->args" \
    "--listen-address[The listen address of the PostgreSQL cluster]:arg:->args" \
    {-u,--username}"[The name of the superuser]:arg:->args" \
    {-P,--password}"[The password of the superuser]:arg:->args" \
    \*{-t,--tag}"[A tag (key/value pair) to add to the cluster]:arg:->args" \
    "*--node-selector[Only create instance(s) on nodes matching the given tag(s)]:arg:->args" \
    \*{-e,--extension}"[The PostgreSQL extension(s) (comma-separated extension names)]:extensions:->extension" \
    {-c,--config}"[The path of PostgreSQL config files]:config:_directories" \
    "--pgdata[The host path to mount PGDATA into PostgreSQL]:pgdata:_directories" \
    "--logs[The directory in which PostgreSQL will create its log files]:logs:_directories" \
    "--wal[The directory in which PostgreSQL will create its WAL files]:logs:_directories" \
    "*--volume[Bind mount a volume from your disk to the PostgreSQL environment (format: <host-path>:<pg-env-path>)]:volume:_files" \
    {-f,--file}"[A YAML file containing a PostgreSQL cluster definition (overrides other options)]:yaml file:_files" \
    {--ha,--high-availability}"[Creates the cluster as high available Postgres cluster (powered by Patroni)]" \
    {-I,--instances}"[The number of HA PostgreSQL instances to create]:arg:->args" \
    "--no-tls[Disables TLS encryption for PostgreSQL connections]" \
    {-i,--interactive}"[Prompts for the cluster information interactively (no other parameters are required)]" \
    "*::arg:->args"

    case $state in
        extension)
            local version="16.3"
            local flavor="postgres"
            i=0
            until [ $i -eq ${#words[@]} ]
            do
                word=${words[$i]}
                if [[ "$word" = "-v" ]] || [[ "$word" = "--version" ]]; then
                    version=${words[((i+1))]}
                fi
                if [[ "$word" = "-F" ]] || [[ "$word" = "--flavor" ]]; then
                    flavor=${words[((i+1))]}
                fi
                ((i++))
            done
            __stackgres_list_extensions
            ;;
    esac
}

function _stackgres_cluster_delete() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-E,--environment}"[The target environment id]:environment:__stackgres_list_environments" \
    "--context[Use a named context]:context:__stackgres_list_contexts" \
    {-t,--tag}"[Only delete clusters that are tagged accordingly]:arg:->args" \
    {-f,--force}"[Force deletion (doesn't ask for confirmation)]" \
    {-a,--all}"[Delete all clusters]" \
    "1:cluster:->cluster" \
    "*::arg:->args"

    case $state in
        cluster)
            for arg in "${words[@]}"; do
                if [[ "$arg" = "-a" ]] || [[ "$arg" = "--all" ]] || [[ "$arg" = "-t" ]] || [[ "$arg" = "--tag" ]]; then
                    return;
                fi
            done
            __stackgres_list_all_clusters
            ;;
    esac
}

function _stackgres_cluster_start() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-E,--environment}"[The target environment id]:environment:__stackgres_list_environments" \
    "--context[Use a named context]:context:__stackgres_list_contexts" \
    {-t,--tag}"[Only start clusters that are tagged accordingly]:arg:->args" \
    {-a,--all}"[Start all stopped clusters]" \
    "1:cluster:->cluster" \
    "*::arg:->args"

    case $state in
        cluster)
            for arg in "${words[@]}"; do
                if [[ "$arg" = "-a" ]] || [[ "$arg" = "--all" ]] || [[ "$arg" = "-t" ]] || [[ "$arg" = "--tag" ]]; then
                    return;
                fi
            done
            __stackgres_list_all_clusters
            ;;
    esac
}

function _stackgres_cluster_stop() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-E,--environment}"[The target environment id]:environment:__stackgres_list_environments" \
    "--context[Use a named context]:context:__stackgres_list_contexts" \
    {-t,--tag}"[Only stop clusters that are tagged accordingly]:arg:->args" \
    {-a,--all}"[Stop all clusters]" \
    "1:cluster:->cluster" \
    "*::arg:->args"

    case $state in
        cluster)
            for arg in "${words[@]}"; do
                if [[ "$arg" = "-a" ]] || [[ "$arg" = "--all" ]] || [[ "$arg" = "-t" ]] || [[ "$arg" = "--tag" ]]; then
                    return;
                fi
            done
            __stackgres_list_all_clusters
            ;;
    esac
}

function _stackgres_cluster_restart() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-E,--environment}"[The target environment id]:environment:__stackgres_list_environments" \
    "--context[Use a named context]:context:__stackgres_list_contexts" \
    {-t,--tag}"[Only restart clusters that are tagged accordingly]:arg:->args" \
    {-a,--all}"[Restart all clusters]" \
    "1:cluster:->cluster" \
    "*::arg:->args"

    case $state in
        cluster)
            for arg in "${words[@]}"; do
                if [[ "$arg" = "-a" ]] || [[ "$arg" = "--all" ]] || [[ "$arg" = "-t" ]] || [[ "$arg" = "--tag" ]]; then
                    return;
                fi
            done
            __stackgres_list_all_clusters
            ;;
    esac
}

function _stackgres_cluster_logs() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-f,--follow}"[Stream the logs]" \
    {-i,--instance}"[The PostgreSQL instance name for high available clusters]:instance:->instances" \
    {-c,--component}"[Log component]:component:(postgres patroni slon etcd)" \
    "1:cluster:__stackgres_list_all_clusters" \
    "*::arg:->args"

    case $state in
      instances)
        skip_next=0
        for (( i = 2; i <= ${#words}; i++ )); do

          if (( i == CURRENT && skip_next )); then
            skip_next=0
            continue
          fi

          word=${words[i]}

          case $word in
            -i|--instance)
              skip_next=1
              continue
              ;;
            -f|--follow|-h|--help)
              continue
              ;;
            -*)
              continue
              ;;
          esac

          cluster=$word
          break
        done

        if [[ -n $cluster ]]; then
          __stackgres_cluster_list_instances
        fi
      ;;
    esac
}

function _stackgres_cluster_exec() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-i,--instance}"[The PostgreSQL instance name for high available clusters]:instance:->instances" \
    "1:cluster:__stackgres_list_all_clusters" \
    "2:separator:(--)" \
    "*::arg:->args"

    case $state in
      instances)
        skip_next=0
        for (( i = 2; i <= ${#words}; i++ )); do

          if (( i == CURRENT && skip_next )); then
            skip_next=0
            continue
          fi

          word=${words[i]}

          case $word in
            -i|--instance)
              skip_next=1
              continue
              ;;
            -h|--help)
              continue
              ;;
            --)
              break
              ;;
            -*)
              continue
              ;;
          esac

          cluster=$word
          break
        done

        if [[ -n $cluster ]]; then
          __stackgres_cluster_list_instances
        fi
      ;;
    esac
}

function _stackgres_cluster_psql() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-d,--dbname}"[Database name to connect to]:dbname:->args" \
    {-i,--instance}"[The PostgreSQL instance name for high available clusters]:instance:->instances" \
    "--target[Which listener to connect to]:target:(postgres ivorysql)" \
    "1:cluster:__stackgres_list_all_clusters" \
    "*::arg:->args"

    case $state in
      instances)
        skip_next=0
        for (( i = 2; i <= ${#words}; i++ )); do

          if (( i == CURRENT && skip_next )); then
            skip_next=0
            continue
          fi

          word=${words[i]}

          case $word in
            -i|--instance|-d|--dbname)
              skip_next=1
              continue
              ;;
            -h|--help)
              continue
              ;;
            -*)
              continue
              ;;
          esac

          cluster=$word
          break
        done

        if [[ -n $cluster ]]; then
          __stackgres_cluster_list_instances
        fi
      ;;
    esac
}

function _stackgres_cluster_tunnel() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-p,--port}"[Local port to listen on (default: 15432 for Postgres, 11521 for IvorySQL)]:port:->args" \
    {-b,--bind}"[Bind address (default: 127.0.0.1)]:address:->args" \
    {-i,--instance}"[The PostgreSQL instance name for high available clusters]:instance:->instances" \
    "--target[Which listener to tunnel to (default: matches the cluster's flavor)]:target:(postgres ivorysql)" \
    "1:cluster:__stackgres_list_all_clusters" \
    "*::arg:->args"

    case $state in
      instances)
        skip_next=0
        for (( i = 2; i <= ${#words}; i++ )); do

          if (( i == CURRENT && skip_next )); then
            skip_next=0
            continue
          fi

          word=${words[i]}

          case $word in
            -i|--instance|-p|--port|-b|--bind)
              skip_next=1
              continue
              ;;
            -h|--help)
              continue
              ;;
            -*)
              continue
              ;;
          esac

          cluster=$word
          break
        done

        if [[ -n $cluster ]]; then
          __stackgres_cluster_list_instances
        fi
      ;;
    esac
}

function _stackgres_cluster_version() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1: :(list)" \
    "*::arg:->args"

  case $line[1] in
    list)
      _stackgres_cluster_version_list
    ;;
  esac

}

function _stackgres_cluster_extension() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1: :(list)" \
    "*::arg:->args"

  case $line[1] in
    list)
      _stackgres_cluster_extension_list
    ;;
  esac

}

function _stackgres_cluster_version_list() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-F,--flavor}"[The database flavor]:flavor:(postgres ivorysql)" \
    "*::arg:->args"

}

function _stackgres_cluster_extension_list() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-F,--flavor}"[The database flavor]:flavor:(postgres ivorysql)" \
    {-v,--version}"[The PostgreSQL version for which to list the extensions]:version:__stackgres_list_versions" \
    {-V,--include-versions}"[Include the extension versions]" \
    "*::arg:->args"

}

function _stackgres_cluster_metrics() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1: :(checkpoints)" \
    "*::arg:->args"

  case $line[1] in
    checkpoints)
      _stackgres_cluster_metrics_checkpoints
    ;;
  esac

}

function _stackgres_cluster_metrics_checkpoints() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-i,--instance}"[Limit to a specific instance]:arg:->args" \
    {-d,--database}"[Filter to a single Postgres database]:arg:->args" \
    "--since[Start of time window (e.g. 1h, 24h, 7d, or ISO-8601)]:arg:->args" \
    "--until[End of time window]:arg:->args" \
    "--limit[Max checkpoints to fetch]:arg:->args" \
    "--format[Output format]:format:(summary table json sparkline)" \
    "1:cluster:__stackgres_list_all_clusters" \
    "*::arg:->args"

}

function _stackgres_node() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1: :(get list delete tag)" \
    "*::arg:->args"

  case $line[1] in
    get)
      _stackgres_node_get
    ;;
    list)
      _stackgres_node_list
    ;;
    delete)
      _stackgres_node_delete
    ;;
    tag)
      _stackgres_node_tag
    ;;
  esac
}

function _stackgres_node_get() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1:node:__stackgres_list_all_nodes" \
    "*::arg:->args"

}

function _stackgres_node_list() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-q,--quiet}"[Only display the node IDs]" \
    "--show-tags[Shows the node tags in the list]" \
    \*{-t,--tag}"[Only list nodes that are tagged accordingly]:arg:->args" \
    "*::arg:->args"

}

function _stackgres_node_delete() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-f,--force}"[Force deletion (doesn't ask for confirmation)]" \
    "1:node:__stackgres_list_all_nodes" \
    "*::arg:->args"

}

function _stackgres_node_tag() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1: :(add remove)" \
    "*::arg:->args"

  case $line[1] in
    add)
      _stackgres_node_tag_add
    ;;
    remove)
      _stackgres_node_tag_remove
    ;;
  esac
}

function _stackgres_node_tag_add() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1:node:__stackgres_list_all_nodes" \
    "*::arg:->args"

}

function _stackgres_node_tag_remove() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1:node:__stackgres_list_all_nodes" \
    "*::arg:->args"

}

function _stackgres_slon() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1: :(get list)" \
    "*::arg:->args"

  case $line[1] in
    get)
      _stackgres_slon_get
    ;;
    list)
      _stackgres_slon_list
    ;;
  esac

}

function _stackgres_slon_get() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1:slon:__stackgres_list_all_slons" \
    "*::arg:->args"

}

function _stackgres_slon_list() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-q,--quiet}"[Only display the slon IDs]" \
    "*::arg:->args"

}

function _stackgres_whoami() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "*::arg:->args"

}

function _stackgres_environment() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1: :(list get use delete)" \
    "*::arg:->args"

  case $line[1] in
    list)
      _stackgres_environment_list
    ;;
    get)
      _stackgres_environment_get
    ;;
    use)
      _stackgres_environment_use
    ;;
    delete|rm)
      _stackgres_environment_delete
    ;;
  esac

}

function _stackgres_environment_list() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-q,--quiet}"[Only display the environment IDs]" \
    "*::arg:->args"

}

function _stackgres_environment_get() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1:environment:__stackgres_list_environments" \
    "*::arg:->args"

}

function _stackgres_environment_use() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1:environment:__stackgres_list_environments" \
    "*::arg:->args"

}

function _stackgres_environment_delete() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    {-f,--force}"[Force deletion (doesn't ask for confirmation)]" \
    "1:environment:__stackgres_list_environments" \
    "*::arg:->args"

}

function _stackgres_context() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1: :(list current use set remove)" \
    "*::arg:->args"

  case $line[1] in
    list)
      _stackgres_context_list
    ;;
    current)
      _stackgres_context_current
    ;;
    use)
      _stackgres_context_use
    ;;
    set)
      _stackgres_context_set
    ;;
    remove|rm|delete)
      _stackgres_context_remove
    ;;
  esac

}

function _stackgres_context_list() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "*::arg:->args"

}

function _stackgres_context_current() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "*::arg:->args"

}

function _stackgres_context_use() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1:context:__stackgres_list_contexts" \
    "*::arg:->args"

}

function _stackgres_context_set() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "--endpoint[The matriarch/cloud gRPC endpoint]:arg:->args" \
    "--token[Bearer token for authentication]:arg:->args" \
    "--tls[Force TLS on/off (default: auto)]:tls:(true false)" \
    "--default-env[The environment this context targets by default]:environment:__stackgres_list_environments" \
    "1:context:__stackgres_list_contexts" \
    "*::arg:->args"

}

function _stackgres_context_remove() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1:context:__stackgres_list_contexts" \
    "*::arg:->args"

}

function _stackgres_login() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "--endpoint[The cloud endpoint (host[:port])]:arg:->args" \
    "--name[Name for the saved context (default: the endpoint host)]:arg:->args" \
    "1:one-time-token:->args" \
    "*::arg:->args"

}

function _stackgres_logout() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "1:context:__stackgres_list_contexts" \
    "*::arg:->args"

}

function _stackgres_status() {
  local line

  _arguments -C \
    {-h,--help}"[Display help]" \
    "*::arg:->args"

}



__stackgres_list_all_clusters() {
   declare -a clusters
   clusters=($(stackgres cluster list -q 2> /dev/null)) || return 1
   _describe -t clusters "all clusters" clusters "$@"
   return 0
}

__stackgres_list_versions() {
   declare -a versions
   local _flavor="${flavor:-postgres}"
   versions=($(stackgres cluster version list -F "$_flavor" 2> /dev/null)) || return 1
   _describe -t versions "available versions" versions "$@"
   return 0
}

__stackgres_list_extensions() {
   declare -a extensions line
   local _flavor="${flavor:-postgres}"

   extensions=($(stackgres cluster extension list -V -F "$_flavor" -v $version 2> /dev/null)) || return 1
   _describe -t extensions "available extensions" extensions "$@"
   return 0
}

__stackgres_list_all_nodes() {
   declare -a nodes
   nodes=($(stackgres node list -q 2> /dev/null)) || return 1
   _describe -t nodes "all nodes" nodes "$@"
   return 0
}

__stackgres_list_all_slons() {
   declare -a slons
   slons=($(stackgres slon list -q 2> /dev/null)) || return 1
   _describe -t slons "all slons" slons "$@"
   return 0
}

__stackgres_cluster_list_instances() {
  declare -a instances
  instances=($(stackgres cluster get --instances-only $cluster 2> /dev/null)) || return 1
  _describe -t instances "all instances" instances "$@"
  return 0
}

__stackgres_list_environments() {
   declare -a environments
   environments=($(stackgres environment list -q 2> /dev/null)) || return 1
   _describe -t environments "environments" environments "$@"
   return 0
}

__stackgres_list_contexts() {
   declare -a contexts
   contexts=($(stackgres context list 2> /dev/null | tail -n +2 | sed 's/^[* ]*//' | awk '{print $1}')) || return 1
   _describe -t contexts "contexts" contexts "$@"
   return 0
}