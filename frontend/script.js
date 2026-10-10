
const API_URL = "http://localhost:8080";

const repositoryForm = document.getElementById("repositoryForm");
const repositoryMessage = document.getElementById("repositoryMessage");
const repositoryIdInput = document.getElementById("repositoryId");
const askButton = document.getElementById("askButton");

let connectedRepositoryId = null;

// Connect GitHub repository
repositoryForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    repositoryMessage.textContent = "Connecting and indexing repository...";

    const name = document.getElementById("repositoryName").value.trim();
    const githubUrl = document.getElementById("githubUrl").value.trim();

    try {
        const response = await fetch(`${API_URL}/api/repositories`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name: name,
                githubUrl: githubUrl
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(
                data.message || data.detail || JSON.stringify(data)
            );
        }

        connectedRepositoryId = data.id;
        repositoryIdInput.value = data.id;

        repositoryMessage.textContent =
            `Repository connected! ID: ${data.id}, Status: ${data.status}`;

    } catch (error) {
        repositoryMessage.textContent =
            "Connection failed: " + error.message;
    }
});

// Ask a question about the repository
askButton.addEventListener("click", async function () {
    const repositoryId = repositoryIdInput.value.trim();
    const question = document.getElementById("question").value.trim();

    const askMessage = document.getElementById("askMessage");
    const answer = document.getElementById("answer");
    const sources = document.getElementById("sources");
    const answerBox = document.getElementById("answerBox");

    if (!repositoryId || !question) {
        askMessage.textContent =
            "Enter a repository ID and your question.";
        return;
    }

    askButton.disabled = true;
    askMessage.textContent = "Thinking...";
    answerBox.style.display = "none";
    sources.replaceChildren();

    try {
        const response = await fetch(`${API_URL}/api/ask`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                repositoryId: repositoryId,
                question: question
            })
        });

        const data = await response.json();

        console.log("ASK API RESPONSE:", data);

        if (!response.ok) {
            throw new Error(
                data.message || data.detail || JSON.stringify(data)
            );
        }

        // The Spring Boot endpoint returns the AI response as JSON.
        const result = data;

        // Display the actual AI-generated answer.
        const answerText =
            typeof result.answer === "string" ? result.answer : "";

        if (!answerText) {
            console.error("Missing answer field. Received:", result);
            throw new Error(
                "The API response does not contain an answer string. Check the browser console."
            );
        }

        answer.textContent = answerText;

        // Display source files only when sources is an array.
        sources.replaceChildren();

        const sourceList = Array.isArray(result.sources)
            ? result.sources
            : [];

        sourceList.forEach(function (source) {
            const card = document.createElement("div");
            card.className = "source";

            const filename = document.createElement("strong");
            filename.textContent =
                source.file_path || source.file_name || "Source file";
            card.appendChild(filename);

            const snippet = document.createElement("pre");
            snippet.textContent = source.snippet || "";
            card.appendChild(snippet);

            sources.appendChild(card);
        });

        answerBox.style.display = "block";
        askMessage.textContent = "Answer received!";

    } catch (error) {
        console.error("Ask AI error:", error);
        askMessage.textContent = "Question failed: " + error.message;
    } finally {
        askButton.disabled = false;
    }
});
