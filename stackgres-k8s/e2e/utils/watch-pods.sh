new_state(){

  clear
  kubectl get pods --all-namespaces
  echo ""
}

while true;do
  new_state
  sleep 2
done
