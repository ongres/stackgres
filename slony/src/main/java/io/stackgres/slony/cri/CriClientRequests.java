package io.stackgres.slony.cri;

import com.google.common.base.Strings;
import runtime.v1.Api;
import runtime.v1.ImageServiceGrpc;
import runtime.v1.RuntimeServiceGrpc;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class CriClientRequests {

    private final String criSocketPath;

    public CriClientRequests(String criSocketPath) {
        this.criSocketPath = criSocketPath;
    }

    public List<Api.PodSandbox> listPods(Map<String, String> labels, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        Api.ListPodSandboxRequest request = Api.ListPodSandboxRequest.newBuilder()
                .setFilter(Api.PodSandboxFilter.newBuilder().putAllLabelSelector(labels).build())
                .build();
        try {
            CompletableFutureFromStreamObserver<Api.ListPodSandboxResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.listPodSandbox(request, future);
            return future.get().getItemsList();
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    private CriContext context() {
        return new CriContext(criSocketPath);
    }

    public Api.PodSandboxStatus getPod(RuntimeServiceGrpc.RuntimeServiceStub runtimeService, String podId) {
        try {
            Api.PodSandboxStatusRequest request = Api.PodSandboxStatusRequest.newBuilder().setPodSandboxId(podId).build();

            CompletableFutureFromStreamObserver<Api.PodSandboxStatusResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.podSandboxStatus(request, future);
            Api.PodSandboxStatusResponse response = future.get();

            Api.PodSandboxStatus p = response.getStatus();
            return p;
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public boolean imageExists(String image, ImageServiceGrpc.ImageServiceStub imageService) {
        try {
            CompletableFutureFromStreamObserver<Api.ImageStatusResponse> future = new CompletableFutureFromStreamObserver<>();
            Api.ImageStatusRequest request = Api.ImageStatusRequest.newBuilder().setImage(Api.ImageSpec.newBuilder().setImage(image).build()).build();
            imageService.imageStatus(request, future);

            return !Strings.isNullOrEmpty(future.get().getImage().getId());
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public String getImageRef(String image, ImageServiceGrpc.ImageServiceStub imageService) {
        try {
            CompletableFutureFromStreamObserver<Api.ImageStatusResponse> future = new CompletableFutureFromStreamObserver<>();
            Api.ImageStatusRequest request = Api.ImageStatusRequest.newBuilder().setImage(Api.ImageSpec.newBuilder().setImage(image).build()).build();
            imageService.imageStatus(request, future);

            return future.get().getImage().getId();
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public String pullImage(String image, ImageServiceGrpc.ImageServiceStub imageService) {
        try {
            CompletableFutureFromStreamObserver<Api.PullImageResponse> future = new CompletableFutureFromStreamObserver<>();
            Api.PullImageRequest request = Api.PullImageRequest.newBuilder().setImage(Api.ImageSpec.newBuilder().setImage(image).build()).build();
            imageService.pullImage(request, future);
            return future.get().getImageRef();
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public String createPod(Api.PodSandboxConfig podSandboxConfig, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        try {
            Api.RunPodSandboxRequest request = Api.RunPodSandboxRequest.newBuilder()
                    .setConfig(podSandboxConfig)
                    .build();

            CompletableFutureFromStreamObserver<Api.RunPodSandboxResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.runPodSandbox(request, future);

            return future.get().getPodSandboxId();
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public String createContainer(String podId, Api.ContainerConfig containerConfig, Api.PodSandboxConfig podSandboxConfig, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        try {
            Api.CreateContainerRequest request = Api.CreateContainerRequest.newBuilder()
                    .setPodSandboxId(podId)
                    .setConfig(containerConfig)
                    .setSandboxConfig(podSandboxConfig)
                    .build();

            CompletableFutureFromStreamObserver<Api.CreateContainerResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.createContainer(request, future);
            return future.get().getContainerId();
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public void startContainer(RuntimeServiceGrpc.RuntimeServiceStub runtimeService, String containerId) {
        try {
            Api.StartContainerRequest request = Api.StartContainerRequest.newBuilder()
                    .setContainerId(containerId)
                    .build();

            CompletableFutureFromStreamObserver<Api.StartContainerResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.startContainer(request, future);
            future.get();
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public Api.ExecSyncResponse execSyncInContainer(String containerId, List<String> cmds, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        try {
            Api.ExecSyncRequest execRequest = Api.ExecSyncRequest.newBuilder()
                    .setContainerId(containerId)
                    .addAllCmd(cmds)
                    .build();

            CompletableFutureFromStreamObserver<Api.ExecSyncResponse> execFuture = new CompletableFutureFromStreamObserver<>();
            runtimeService.execSync(execRequest, execFuture);

            return execFuture.get();
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public void printContainerLogs(String containerId, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        try {
            Api.ContainerStatusRequest request = Api.ContainerStatusRequest.newBuilder().setContainerId(containerId).build();

            CompletableFutureFromStreamObserver<Api.ContainerStatusResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.containerStatus(request, future);

            System.out.println(Files.readString(Paths.get(future.get().getStatus().getLogPath())));
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public List<Api.Container> listPodContainers(String podId, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        Api.ListContainersRequest request = Api.ListContainersRequest.newBuilder()
                .setFilter(Api.ContainerFilter.newBuilder().setPodSandboxId(podId).build())
                .build();
        return listContainers(runtimeService, request);
    }

    public List<Api.Container> listContainers(RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        Api.ListContainersRequest request = Api.ListContainersRequest.newBuilder().build();
        return listContainers(runtimeService, request);
    }

    private List<Api.Container> listContainers(RuntimeServiceGrpc.RuntimeServiceStub runtimeService, Api.ListContainersRequest request) {
        try {
            CompletableFutureFromStreamObserver<Api.ListContainersResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.listContainers(request, future);

            return future.get().getContainersList();

        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public void stopContainer(String containerId, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        try {
            Api.StopContainerRequest request = Api.StopContainerRequest.newBuilder().setContainerId(containerId).build();
            CompletableFutureFromStreamObserver<Api.StopContainerResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.stopContainer(request, future);
            future.get();

        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public void removePod(String podId, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        try {
            Api.RemovePodSandboxRequest request = Api.RemovePodSandboxRequest.newBuilder().setPodSandboxId(podId).build();
            CompletableFutureFromStreamObserver<Api.RemovePodSandboxResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.removePodSandbox(request, future);
            future.get();

        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public void removeContainer(String containerId, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        try {
            Api.RemoveContainerRequest request = Api.RemoveContainerRequest.newBuilder().setContainerId(containerId).build();
            CompletableFutureFromStreamObserver<Api.RemoveContainerResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.removeContainer(request, future);
            future.get();

        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

    public Api.ContainerStatusResponse getContainerStatus(String containerId, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        try {
            Api.ContainerStatusRequest request = Api.ContainerStatusRequest.newBuilder().setContainerId(containerId).build();
            CompletableFutureFromStreamObserver<Api.ContainerStatusResponse> future = new CompletableFutureFromStreamObserver<>();
            runtimeService.containerStatus(request, future);
            return future.get();
        } catch (Exception e) {
            throw new CriClientException(e, context());
        }
    }

}