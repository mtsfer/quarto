const infoElement = document.querySelector(".informations")
const bodyElement = document.querySelector(".body")
let boardElement = null
let bufferElement = null

let turn = null
let player = null
let turnPiece = null
let boardLength = null
let gameBoardData = null
let bufferBoardData = null

const websocket = new WebSocket("ws://localhost:8081/quarto/api")

websocket.onopen = onOpen
websocket.onmessage = onMessage

function onOpen() {
    const startButton = document.createElement("button")
    startButton.textContent = "Iniciar"
    startButton.onclick = startGame
    infoElement.appendChild(startButton)
}

function onMessage(message) {
    const response = JSON.parse(message.data)
    const status = response["status"]
    switch (status) {
        case "PREPARING":
            player = response["player"]

            const waitingMessage = document.createElement("h4")
            waitingMessage.textContent = "Waiting your opponent..."
            infoElement.appendChild(waitingMessage)

            break
        case "IN_PROGRESS":
            turn = response["turn"]

            const turnPieceCoordinate = response["turnPieceCoordinate"]
            turnPiece = turnPieceCoordinate ? getPieceFromCoordinates(turnPieceCoordinate) : null

            if (!turnPieceCoordinate) {
                removeChildren(bodyElement)
                gameBoardData = response["board"]
                bufferBoardData = response["buffer"]

                boardLength = gameBoardData.length

                constructGameSection(gameBoardData)
                constructBufferSection(bufferBoardData)
            }

            const turnInfo = document.createElement("h5")
            if (player !== turn) {
                turnInfo.textContent = "It's your OPPONENT's turn!"
            } else if (turnPiece === null) {
                turnInfo.textContent = "Select a piece on the buffer for your opponent..."
            } else {
                turnPiece.dataset.state = "selected"
                turnInfo.textContent = "It's YOUR turn!"
            }
            infoElement.lastChild.remove()
            infoElement.appendChild(turnInfo)

            document.addEventListener("click", handleMouseClick)

            break
        case "FINISHED":
            const winner = response["winner"]

            const winnerInfo = document.createElement("h5")
            if (winner === player) {
                winnerInfo.textContent = "Congratulations, you win!"
            } else if (winner === "DRAW") {
                winnerInfo.textContent = "Draw!"
            } else {
                winnerInfo.textContent = "Oh...You lost!"
            }
            infoElement.lastChild.remove()
            infoElement.appendChild(winnerInfo)

            stopInteraction()

            const playAgainButton = document.createElement("button")
            playAgainButton.type = "button"
            playAgainButton.textContent = "Play Again!"
            playAgainButton.addEventListener("click", () => location.reload())
            infoElement.appendChild(playAgainButton)

            break
    }
}

function startGame() {
    infoElement.lastChild.remove()
    const joinRequest = JSON.stringify({ connectionType: "ENTER_GAME" })
    websocket.send(joinRequest)
}

function constructGameSection(data) {
    const boardSection = document.createElement("div")
    boardSection.className = "board-section"

    const boardTitle = document.createElement("caption")
    boardTitle.textContent = "Quarto!"
    boardSection.appendChild(boardTitle)

    boardElement = constructBoard("board", data)
    boardSection.appendChild(boardElement)

    bodyElement.appendChild(boardSection)
}

function constructBufferSection(data) {
    const bufferSection = document.createElement("div")
    bufferSection.className = "buffer-section"

    const bufferTitle = document.createElement("caption")
    bufferTitle.textContent = "Buffer"
    bufferSection.appendChild(bufferTitle)

    bufferElement = constructBoard("buffer", data)
    bufferSection.appendChild(bufferElement)

    bodyElement.appendChild(bufferSection)
}

function constructBoard(name, data) {
    const board = document.createElement("div")
    board.className = "board"

    populateBoard(name, board, data)

    return board
}

function populateBoard(name, board, data) {
    for (let row = 0; row < boardLength; row++) {
        for (let column = 0; column < boardLength; column++) {
            const currentPiece = data[row][column]

            const tile = document.createElement("div")
            tile.classList.add("tile", `${name.toLowerCase()}-tile`)
            tile.dataset.index = (boardLength * row + column).toString()

            if (currentPiece !== null) {
                const image = constructImageFromPieceSpecs(currentPiece)
                tile.appendChild(image)
            }

            board.appendChild(tile)
        }
    }
}

function handleMouseClick(evt) {
    const target = evt.target
    const targetClassList = target.classList

    if (targetClassList.contains("tile")) {
        handleTileClick(target)
    } else if (targetClassList.contains("piece")) {
        handleTileClick(target.parentNode)
    }
}

function handleTileClick(tile) {
    if (player !== turn) {
        infoElement.firstChild.remove()
        const turnWarning = document.createElement("h5")
        turnWarning.textContent = "Wait for your turn..."
        infoElement.appendChild(turnWarning)
        return
    }

    const tileClasses = tile.classList

    if (turnPiece === null && tileClasses.contains("buffer-tile") && tile.hasChildNodes()) {
        turnPiece = tile
        turnPiece.dataset.state = "selected"
        const pieceCoordinates = getTileCoordinates(turnPiece)
        const setPieceRequest = JSON.stringify({
            pieceCoordinate: { x: pieceCoordinates[0], y: pieceCoordinates[1] },
            connectionType: "SET_PIECE"
        })
        websocket.send(setPieceRequest)
    } else if (turnPiece !== null && tileClasses.contains("board-tile") && !tile.hasChildNodes()) {
        const pieceCoordinates = getTileCoordinates(tile)
        const placeOnBoardRequest = JSON.stringify({
            pieceCoordinate: { x: pieceCoordinates[0], y: pieceCoordinates[1] },
            connectionType: "PLACE_ON_BOARD"
        })
        websocket.send(placeOnBoardRequest);
    }
}

function getTileCoordinates(tile) {
    const rowIndex = ~~(tile.dataset.index / boardLength)
    const columnIndex = (tile.dataset.index % boardLength)
    return [rowIndex, columnIndex]
}

function getPieceFromCoordinates(coordinates) {
    const bufferChildren = bufferElement.children
    const index = coordinates["x"] * boardLength + coordinates["y"]
    return bufferChildren.item(index)
}

function removeChildren(node) {
    while (node.firstChild) {
        node.removeChild(node.firstChild)
    }
}

function constructImageFromPieceSpecs(piece) {
    const shape = piece["shape"]
    const color = piece["color"]
    const size = piece["size"]

    const type = piece["type"]
    const image = document.createElement("img")
    image.className = "piece"
    image.src = `images/${shape[0]}${color[0]}${size[0]}${type[0]}.svg`

    return image
}

function stopInteraction() {
    document.removeEventListener("click", handleMouseClick)
}
