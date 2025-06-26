class ThreeJSBackground {
    constructor(containerId = 'threejs-bg') {
        // 1. Setup
        this.container = document.getElementById(containerId);
        this.scene = new THREE.Scene();
        this.camera = new THREE.PerspectiveCamera(75, window.innerWidth/window.innerHeight, 0.1, 1000);
        this.renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
        
        // 2. Configure renderer
        this.renderer.setSize(this.container.clientWidth, this.container.clientHeight);
        this.container.appendChild(this.renderer.domElement);
        
        // 3. Create objects
        this.createObjects();
        this.setupLights();
        this.setupCamera();
        
        // 4. Start animation
        this.animate();
        
        // 5. Handle resize
        window.addEventListener('resize', () => this.onResize());
    }

    createObjects() {
        // Main floating object
        const geometry = new THREE.IcosahedronGeometry(2, 1);
        const material = new THREE.MeshPhongMaterial({ 
            color: 0x4f46e5,
            transparent: true,
            opacity: 0.7
        });
        this.mainObject = new THREE.Mesh(geometry, material);
        this.scene.add(this.mainObject);

        // Smaller floating objects
        this.floatingObjects = [];
        for (let i = 0; i < 8; i++) {
            const size = 0.5 + Math.random() * 1.5;
            const geo = new THREE.SphereGeometry(size, 16, 16);
            const mat = new THREE.MeshBasicMaterial({
                color: 0x818cf8,
                transparent: true,
                opacity: 0.5
            });
            const mesh = new THREE.Mesh(geo, mat);
            mesh.position.set(
                (Math.random() - 0.5) * 20,
                (Math.random() - 0.5) * 10,
                Math.random() * -50
            );
            this.scene.add(mesh);
            this.floatingObjects.push(mesh);
        }
    }

    setupLights() {
        const light1 = new THREE.DirectionalLight(0xffffff, 0.5);
        light1.position.set(1, 1, 1);
        this.scene.add(light1);

        const light2 = new THREE.AmbientLight(0x404040);
        this.scene.add(light2);
    }

    setupCamera() {
        this.camera.position.z = 15;
        this.controls = new THREE.OrbitControls(this.camera, this.renderer.domElement);
        this.controls.enableZoom = false;
        this.controls.autoRotate = true;
        this.controls.autoRotateSpeed = 0.5;
    }

    animate() {
        requestAnimationFrame(() => this.animate());
        
        // Rotate main object
        this.mainObject.rotation.x += 0.005;
        this.mainObject.rotation.y += 0.01;
        
        // Float smaller objects
        this.floatingObjects.forEach(obj => {
            obj.rotation.x += 0.001;
            obj.rotation.y += 0.002;
            obj.position.y = Math.sin(Date.now() * 0.001 + obj.position.x) * 0.5;
        });

        this.controls.update();
        this.renderer.render(this.scene, this.camera);
    }

    onResize() {
        this.camera.aspect = this.container.clientWidth / this.container.clientHeight;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(this.container.clientWidth, this.container.clientHeight);
    }
}

// Initialize automatically when included
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('threejs-bg')) {
        new ThreeJSBackground();
    }
});